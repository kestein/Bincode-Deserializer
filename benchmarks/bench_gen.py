#!/usr/bin/env python3
import csv
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
import sys

# Constants
width = 0.35
records = "param-records"
# CLI parsing
if len(sys.argv) < 2:
    print("Usage: ./bench_gen.py <glob of csv files>")
    sys.exit(1)

# Load in benchmark data
# Name of benchmark -> [stats] implicitly ordered by # records tested on
bench_runs = {}
for csv_file in sys.argv[1:]:
    bench_runs[csv_file] = []
    with open(csv_file, "r") as f:
        csv_rdr = csv.DictReader(f)
        for r in csv_rdr:
            bench_runs[csv_file].append(r)

f, ax = plt.subplots()
ax.set_ylabel("Time (ms)")
ax.set_title("Time to deserialize records")

# Make the rectangles
rects = []
xlabels = []
for run in bench_runs:
    ind = np.arange(len(bench_runs[run]))
    plot_points = [float(x["value"]) for x in bench_runs[run]]
    if len(plot_points) > len(xlabels):
        xlabels = [x[records] for x in bench_runs[run]]
    rects.append(ax.bar(ind - width/len(bench_runs), [float(x["value"]) for x in bench_runs[run]], width, label=run))

ax.set_xticklabels(xlabels)
ax.legend()
f.savefig("benchmarks.png")
