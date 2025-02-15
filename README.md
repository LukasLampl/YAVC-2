# YAVC 2.0 - Video compressor

This program implements a video encoder and decoder based on multiple 
modules like quadtrees, dct and block motion estimation using hexagon 
search and exhaustive search.

Authors: Lukas Lampl; Hans Lampl

## Table of content
1. [Usage](#1-usage)  
    1.1 [Requirements](#11-requirements)  
    2.2 [Command line arguments](#12-command-line-arguments)  
        2.2.1 [Encoding arguments](#121-encoding-arguments)  
        2.2.2 [Decoding arguments](#122-decoding-arguments)  
2. [Modules](#2-modules)  
    2.1 [PixelRaster](#21-pixelraster)  
    2.2 [Macroblocks & partitioning](#22-macroblocks--partitioning)  
    2.3 [Prediction mode evaluator](#23-prediction-mode-evaluator)  
    2.4 [Interprediction](#24-interprediction)  
    2.5 [Intraprediction](#25-intraprediction)  
    2.6 [FCT (fast cosine transform) and Quantization](#26-fct-fast-cosine-transform-and-quantization)  
3. [Comparison to YAVC-1](#3-comparison-to-yavc-1)  

## 1. Usage
### 1.1 Requirements
To run YAVC-2.x smoothly it is recommended to have the following 
system parameters:  

| Part | Requirement |
|:---:|:---------------|
| RAM | 1.0 GB or more |
| CPU | 4 core or more (tested in i7-7700HQ) |
| Memory | depends on video |

> [!NOTE]
> YAVC will be moved to GPU in future releases.

### 1.2 Command line arguments
YAVC-2.x provides several command-line arguments for encoding and decoding.

- For encoding, use the `-encode` flag.
- For decoding, use the `-decode` flag.

Both modes have distinct arguments, which are listed below.

#### 1.2.1 Encoding arguments
| Argument | Description |
|:---------|:------------|
| `-i` | Defines the path to a folder with raw frame images, that should be compressed. Those must be in the format `%04d.bmp`. |
| `-o` | Sets the output directory of the generated YAVCV file. |
| `-auto-adjust` | Enables auto adjusting DCT coefficients (not recommended due to quality loss). |


#### 1.2.2 Decoding arguments

| Argument | Description |
|:---------|:------------|
| `-i` | Sets the input of the YAVCV file to decode. |
| `-o` | Declares the output to where to write the decoded frames. |
| `-playback` | When added the YAVCV will be playedback in a window. |
| `-no-deblock` | Disables the deblocking filter. (Currently recommended) |

## 2. Modules
### 2.1 Pixelraster
The YAVC-2.x software uses a custom made `PixelRaster` class for handling the
input frames. The decision has been made at the YAVC-1 (predecessor) project 
after benchmarking the performance of the native `BufferedImage` against a 
custom implementation.

The custom implementation consistently performed *100%* faster in setting and
retrieving pixel data. For that reason it is continued to be used in 
the YAVC-2.x project.

> [!NOTE]
> The input RGB data is converted into YUV format with `4:2:0` chroma subsampling.

### 2.2 Macroblocks & Partitioning
Macroblocks are used for dividing the input frame into isolated subregions 
that are processed individually. During the encoding process each `MacroBlock` 
will be assigned to a coding mode (inter- or intra prediction). Macroblocks
come in difference sizes reaching from **4x4** up to **128x128** (**4x4** 
-> **8x8** -> **16x16** -> **32x32** -> **64x64** -> **128x128**) this is
needed to capture more spatial data across a frame.

The partitioning of the individual blocks is based on `multiple Quadtrees`. 
The algorithm for dividing the image down is as follows:

1. Divide the image in non-overlapping **128x128** `MacroBlocks`.
2. Calculate the mean color of the current block.
3. Calculate the standard deviation of the blocks color to the mean.
4. If the resulting value ($\sigma_{total}$) is bigger than a set error threshold the block is split into 4 equally sized sub-blocks.
5. Repeat steps 2 - 5, until the standard deviation value is smaller or equal to the error threshold.
  
For each channel (Y, U, V) individually the sum of all standard deviations
is referred as $\sigma_{total}$.  

$$\begin{equation}\sigma_c=\sqrt{\frac{1}{N}\cdot\sum_{m=0}^{N-1}{\sum_{n=0}^{N-1}{(I_{m,n}-\mu)^2}}}\end{equation}$$

$$\begin{equation} \sigma_{total} = \sigma_Y + \sigma_U + \sigma_V \end{equation}$$

$\sigma_c$ : Standard deviation of a channel

$\sigma_Y$ : Standard deviation of the Y-Channel

$\sigma_U$ : Standard deviation of the U-Channel

$\sigma_V$ : Standard deviation of the V-Channel

$N=\begin{cases} \text{Macroblock size for Y-Channel} \\ \frac{\text{Macroblock size}}{2} \text{ for U- and V-Channel} \end{cases}$

$\mu$ : Mean pixel value of the channel

$I_{m,n}$ : Pixel intensity at position *m* and *n* relative to the MacroBlock

Additionaly there is a weighting process that weigths the idividual channels
"importance". Since humans don't perceive color as much as light, the color
component is less crucial, while the luma is weighted more.

### 2.3 Prediction mode evaluator
> [!WARNING]
> The algorithm might and will be changed in future releases to ensure higher reliability.

For B-Frames (Frames with inter- and intra prediction) the prediction mode must
be choosen to ensure the lowest possible residual data at the end of coding.
The current implementation evaluates based on the "flatiness" of a block,
if it is flat, it should be intra predicted, else inter predicted. The current
formula used is as follows:

$$\begin{equation}\Delta_{m,n}=(I_{m,n}-\mu)^2\end{equation}$$

$$\begin{equation}\delta_Y=\frac{1}{max+\varepsilon}\sqrt{\frac{1}{N^2}\cdot\sum_{m=0}^{N-1}{\sum_{n=0}^{N-1}{\Delta_{m,n}}}}\end{equation}$$

$\delta_Y$ : Deviation of the Y-Channel

$N$ : Size of the Macroblock

$\mu$ : Mean Y-intensity

$I_{m,n}$ : Y-intensity at position *m* and *n*

$max$ : Maximum $\Delta_{m,n}$ of the Y-Channel

$\varepsilon$ : Offset to prevent division by $0$.

When the $\delta_Y$ is bigger than a trigger threshold the MacroBlock will be
interpredicted, else intrapredicted.

### 2.4 Interprediction
Interprediction is responsible for finding redundancies across multiple frames
(references). Interprediction consists of searching a Macroblock in a reference
frame that is as close looking as the current block to be predicted. The more
equal the blocks, the lower the residuals and thus a higher compression rate
can be achieved.

YAVC-2.x uses a combination of hexagonal search and exhaustive search to find
matching Macroblocks. Unlike in H.264 the prediction is executed for full pixel
precision. The best match is evaluated using MSE (Mean square error) of all
channels.

At the end a Vector is formed with the span to the X and Y, as well as the position
size and residuals.

### 2.5 Intraprediction
Intraprediction is unlike interprediction responsible for finding redundancies
in the current frame and predict those by extrapolating certain pixels.

YAVC-2.x supports 36 different prediction modes where 3 are planar, horizontal
and vertical. The other prediction modes are from 0° to 180° by an increment of
5°.

Based on the angle the values that are extrapolated will either be at the top
border, left border, bottom border and right border.

At the end a prediction block is formed with the position, size, border pixels
and residuals.

### 2.6 FCT (Fast cosine transform) and Quantization
All residuals will be transformed into a frequency based representation. Since
the native approach of the DCT-II and DCT-III used in YAVC-1.x is to slow for
realtime purposes the YAVC-2.x codec switched to FCT (fast cosine tranform),
which does not calculate the frequency factors, but instead approximates them.

After the transformation the coefficients are quantized and stored. The quantization
step is crucial for eliminating as many non-zero coefficients as possible to
ensure high compression ratios.

## 3. Comparison to YAVC-1
| Feature | YAVC-2.x | YAVC-1.x |
|:--------|:--------:|:--------:|
| GUI | No gui, command line only | Graphical UI for demonstration of school project |
| Macroblock sizes | **4x4**, **8x8**, **16x16**, **32x32**, **64x64**, **128x128** | **4x4**, **8x8**, **16x16**, **32x32**, **64x64** |
| Macroblock partitioning | Quadtree based approach using standard deviation. | Edge detection as division parameter (Sobel-Scharr). |
| Color reduction | Chroma subsampling 4:2:0 | Reduction based on previous frame; Chroma subsampling 4:2:0 |
| Interprediction | For N-Frames (as long as RAM is enough) | Fixed 6 reference frames |
| Intraprediction | With 36 prediction modes | ❌ |
| Discrete cosine transform | FCT for **2x2**, **4x4** and **8x8** | Native DCT for **2x2**, **4x4**, **8x8** |
| Zig-Zag-Coding | For **2x2**, **4x4** and **8x8** | ❌ |
| Scene change detection | ❌ | Histogram based scene change detection |
| B-Frames (inter- and intrapredicted) | ✅ | ✅ |
| M-Frames (no reference; mark frame) | ✅ (future release) | ❌ |
| Audio (future release) | ❌ | ❌ |
| Decoding speed (1920x1080; i7 8-core) | ~35 - 45 ms | ~800 - 1200ms |