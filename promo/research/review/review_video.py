"""Pulls exact frames out of the rendered MP4 into contact sheets and reports stream facts.

Usage: python review_video.py <ffmpeg.exe> <ffprobe.exe> <video.mp4> <out_dir>
"""

import json
import subprocess
import sys
from pathlib import Path

ffmpeg, ffprobe, video, out = sys.argv[1:5]
out_dir = Path(out)
out_dir.mkdir(parents=True, exist_ok=True)

probe = subprocess.run(
    [ffprobe, "-v", "error", "-show_entries", "stream=codec_type,codec_name,width,height,r_frame_rate,nb_frames,duration:format=duration", "-of", "json", video],
    capture_output=True,
    text=True,
    check=True,
).stdout
print(json.dumps(json.loads(probe)))

label = "drawtext=text='f%{n}':x=6:y=6:fontsize=20:fontcolor=red:box=1:boxcolor=white@0.8"


def sheet(select: str, scale: str, tile: str, name: str, single: bool = False) -> None:
    args = [ffmpeg, "-v", "error", "-y", "-i", video, "-vf", f"select='{select}',{scale},{label},{tile}", "-fps_mode", "passthrough"]
    if single:
        args += ["-frames:v", "1"]
    subprocess.run(args + [str(out_dir / name)], check=True)


sheet("not(mod(n,30))", "scale=270:480", "tile=8x2", "mp4_sheet_%02d.jpg")
cuts = [70, 74, 178, 182, 250, 254, 430, 434, 610, 614, 754, 758, 970, 974, 1078, 1082, 1222, 1226, 1366, 1370]
sheet("+".join(f"eq(n,{c})" for c in cuts), "scale=216:384", "tile=10x2", "mp4_cuts.jpg", single=True)
print("sheets:", sorted(p.name for p in out_dir.glob("mp4_*.jpg")))
