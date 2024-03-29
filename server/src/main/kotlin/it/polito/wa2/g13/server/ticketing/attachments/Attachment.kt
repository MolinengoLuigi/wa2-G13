package it.polito.wa2.g13.server.ticketing.attachments

import it.polito.wa2.g13.server.EntityBase
import it.polito.wa2.g13.server.ticketing.messages.Message
import jakarta.persistence.*
import java.util.Date

@Entity
@Table(name = "attachments")
class Attachment(
    @ManyToOne
    @JoinColumn(name = "messageId")
    var message: Message? = null,
    var type: String,
    var attachmentName: String,
    var size: Long,
    var dataBin: ByteArray,
    var datetime: Date,
    setId: Long? = null
) : EntityBase<Long>(setId) {

}

fun AttachmentDTO.toAttachment(message: Message?): Attachment {
    return Attachment(message, type, attachmentName,size, dataBin, datetime, attachmentId)
}