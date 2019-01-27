package com.rong.download;

public class FileTask {
    final String url;
    final String md5;
    final long length;
    final String savePath;
    final String fileName;
    private long countLength = 0;

    public FileTask(String url, String md5, long length, String savePath, String fileName) {
        this.url = url;
        this.md5 = md5;
        this.length = length;
        this.savePath = savePath;
        this.fileName = fileName;
    }

    public void setCountLength(long countLength) {
        this.countLength = countLength;
    }

    public long getCountLength() {
        return countLength;
    }

    @Override
    public String toString() {
        return "FileTask{" +
                "url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                ", length=" + length +
                ", savePath='" + savePath + '\'' +
                ", fileNam='" + fileName + '\'' +
                '}';
    }
}
