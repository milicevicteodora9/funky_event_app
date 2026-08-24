package com.example.funkyeventapp.models;

import java.time.LocalDateTime;

public class ScannedDocument {
    private String id, fileName, fileUri, mimeType;
    private DocumentSource source;
    private LocalDateTime addedAt;
    public ScannedDocument() { }
    public ScannedDocument(String id, String fileName, String fileUri, String mimeType, DocumentSource source, LocalDateTime addedAt) {
        this.id=id; this.fileName=fileName; this.fileUri=fileUri; this.mimeType=mimeType; this.source=source; this.addedAt=addedAt;
    }
    public String getId(){return id;} public void setId(String v){id=v;} public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getFileUri(){return fileUri;} public void setFileUri(String v){fileUri=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;}
    public DocumentSource getSource(){return source;} public void setSource(DocumentSource v){source=v;} public LocalDateTime getAddedAt(){return addedAt;} public void setAddedAt(LocalDateTime v){addedAt=v;}
}
