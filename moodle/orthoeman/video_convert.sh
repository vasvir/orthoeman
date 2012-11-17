#!/bin/sh -e


url="$1";
dst="$2";
ifile=`tempfile -p mdl_ortho`;
curl "$url" -o $ifile;

case "$dst" in
  h264)
	ffmpeg_args='-y -acodec libfaac -ab 128k -vcodec libx264 -preset slow -crf 30 -threads 0 -ar 48000';
	ofile=`tempfile -p mdl_ortho -s .mp4`;
	;;
  ogg)
	ffmpeg_args='-y -f ogg';
	ofile=`tempfile -p mdl_ortho -s .ogg`;
	;;
  webm)
	ffmpeg_args='-y -f webm';
	ofile=`tempfile -p mdl_ortho -s .webm`;
	;;
  *)
        echo "Usage: $0 url {h264|ogg|webm}";
        exit 1
        ;;
esac;

ffmpeg -i $ifile $ffmpeg_args $ofile;
cat $ofile;
rm -rf $ifile $ofile;
