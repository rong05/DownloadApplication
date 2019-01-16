package com.rong.download;

public class FileTmp {
     final long startPath ;
     final long endPath ;
     final String fileName ;
     final String url;
     final int runId;

    public FileTmp(long startPath, long endPath, String fileName, String url, int runId) {
        this.startPath = startPath;
        this.endPath = endPath;
        this.fileName = fileName;
        this.url = url;
        this.runId = runId;
    }

    @Override
    public String toString() {
        return "FileTmp{" +
                "startPath=" + startPath +
                ", endPath=" + endPath +
                ", fileName='" + fileName + '\'' +
                ", url='" + url + '\'' +
                ", runId=" + runId +
                '}';
    }
}
