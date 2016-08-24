#!/usr/bin/env python

from __future__ import print_function
import sys, subprocess
import re

def split_video(input_file, output_base, xsplit=1, ysplit=1):
  ffprobe_out = subprocess.check_output(["ffprobe", "-show_streams", "-loglevel", "quiet", input_file])
  wre = re.compile(ur'width=(\d*)')
  hre = re.compile(ur'height=(\d*)')
  wm = re.search(wre, ffprobe_out)
  hm = re.search(hre, ffprobe_out)
  input_width = int(wm.group(1))
  input_height = int(hm.group(1))
  
  out_width = input_width/xsplit
  out_height = input_height/ysplit

  for x in range(0,xsplit):
    for y in range(0,ysplit):
      #ffmpeg -i input_file -filter:v "crop=out_w:out_h:x:y" out.mp4
      xbase = out_width*x
      ybase = out_height*y
      crop_string = "crop={0}:{1}:{2}:{3}".format(out_width,out_height,xbase,ybase)
      output_filename = "{0}_h{1}_v{2}.mp4".format(output_base, x, y)
      ffmpeg_args = ["ffmpeg", "-i", input_file, "-filter:v", crop_string, output_filename]
      print(ffmpeg_args)
      subprocess.check_output(ffmpeg_args)

if __name__ == "__main__":
  import argparse
  parser = argparse.ArgumentParser(description="Split one large video file into a grid of smaller video files, to be played back spanned across many tablets.")
  parser.add_argument('input_file', help='Input mp4 file to split')
  parser.add_argument('output_base', help='Base of the output filename (h[1]_v[1].mp4 will be appended)')
  parser.add_argument('-x', '--xsplit', help='Number of horizontal times to split the video (1 for no splits)', type=int, default=1)
  parser.add_argument('-y', '--ysplit', help='Number of vertical times to split the video (1 for no splits)', type=int, default=1)
  args = parser.parse_args()
  split_video(args.input_file, args.output_base, args.xsplit, args.ysplit)