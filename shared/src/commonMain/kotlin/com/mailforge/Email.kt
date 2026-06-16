package com.mailforge

data class Email(val from: String,
                 val to:List<String>,
                 val cc: List<String> =emptyList(),
                 val bcc: List<String> =emptyList(),
                 val subject: String,
                 val body: String,
                 val isHtml: Boolean=false,
                 val attachments:List<EmailAttachment> =emptyList(),
                 val replyTo: String?=null)

class EmailAttachment(val filename: String,val data: ByteArray,val mimeType: String="application/octet-stream"){
    override fun equals(other : Any?): Boolean{
        if(this===other) return true
        if(other !is EmailAttachment) return false
        return filename==other.filename && data.contentEquals(other.data)&& mimeType==other.mimeType
    }
    override fun hashCode():Int{
        var result=filename.hashCode()
        result=31*result+data.contentHashCode()
        result=31*result+mimeType.hashCode()
        return result
    }
}