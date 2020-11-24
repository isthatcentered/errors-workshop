package exercises.adts

import framework.RequestBody
import framework.ServerResponse
import framework.respond
import reactor.core.publisher.Mono
import shared.*
import exercises.adts.SendMailReceipt as SendMail
import exercises.adts.SendSMSReceipt as SendSMS


class InviteController(private val inviteProspectUseCase: InviteProspectUseCase) {
    fun create(body: RequestBody<InviteProspectParams>): Mono<ServerResponse<String>> {
        body.validate()

        val prospect = ProspectId(body.data.prospectId)

        return inviteProspectUseCase.runFor(prospect)
            .flatMap { receipt -> respond(200, "All good") }
            .onErrorResume { respond(500, "Something went wrong") }
    }

    // ... other controller routes (index, new, create, show, ...)
}

sealed class InviteProspectReceipt {}

class InviteProspectUseCase(
    private val mail: MailNotifier,
    private val sms: SMSNotifier,
    private val invitations: InvitationService,
    private val flag: FlagService,
) {
    fun runFor(prospect: ProspectId): Mono<InviteProspectReceipt> {
        return invitations
            .generateInviteLinkfor(prospect)
            .flatMap { inviteLinkReceipt -> TODO() }
    }

    private fun sendWelcomeEmail(prospect: ProspectId, inviteLink: InviteLink): Mono<SendMail> {
        val welcomeEmail = MailTemplate(subject = "Hello", body = "World $inviteLink")
        return mail.send(welcomeEmail, prospect)
    }

    private fun sendWelcomeSMS(prospect: ProspectId, inviteLink: InviteLink): Mono<SendSMS> {
        val text = SMSTemplate("Hello $inviteLink")
        return sms.send(text, prospect)
    }
}


sealed class GenerateInviteLinkReceipt {
    data class Success(val link: InviteLink) : GenerateInviteLinkReceipt()
    data class Error(val error: Throwable) : GenerateInviteLinkReceipt()
}

interface InvitationService {
    fun generateInviteLinkfor(propspect: ProspectId): Mono<GenerateInviteLinkReceipt>
}

sealed class MarkProspectReceipt {
    object Success : MarkProspectReceipt()
    data class Error(val error: Throwable) : MarkProspectReceipt()
}

interface FlagService {
    fun mark(prospect: ProspectId): Mono<MarkProspectReceipt>
}


sealed class SendMailReceipt {
    object Success : SendMail()
    object ProspectNotFound : SendMail()
    object ProspectHasNoEmail : SendMail()
    data class Error(val error: Throwable) : SendMail()
}

interface MailNotifier {
    fun send(mail: MailTemplate, to: ProspectId): Mono<SendMail>
}

sealed class SendSMSReceipt {
    object Success : SendSMS()
    object ProspectNotFound : SendSMS()
    object ProspectHasNoPhoneNumber : SendSMS()
    data class Error(val error: Throwable) : SendSMS()
}

interface SMSNotifier {
    fun send(sms: SMSTemplate, user: ProspectId): Mono<SendSMS>
}
