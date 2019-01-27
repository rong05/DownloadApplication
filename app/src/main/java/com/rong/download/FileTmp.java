package com.rong.download;

public class FileTmp {
     final long startPath ;
     final long endPath ;
     final String fileName ;
     final String downloadUrl;
     final String tagUrl;
     final int runId;
    final String savePath;

    public FileTmp(long startPath, long endPath, String fileName, String downloadUrl,String tagUrl, int runId,String savePath) {
        this.startPath = startPath;
        this.endPath = endPath;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.tagUrl = tagUrl;
        this.runId = runId;
        this.savePath = savePath;
    }

    @Override
    public String toString() {
        return "FileTmp{" +
                "startPath=" + startPath +
                ", endPath=" + endPath +
                ", fileName='" + fileName + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", tagUrl='" + tagUrl + '\'' +
                ", runId=" + runId +
                ", savePath='" + savePath + '\'' +
                '}';
    }
}
