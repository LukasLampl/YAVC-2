if [ "$1" == "" ]; then
  video="vid"
else
  video="$1"
fi
java -Xms4G -Xmx8G -jar app/build/libs/yavc_2.0.jar -decode -i ~/work/workspaces/output/${video}.yavcv -playback
