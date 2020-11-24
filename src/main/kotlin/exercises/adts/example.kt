package exercises.adts

import framework.RequestBody
import framework.ServerResponse
import framework.respond
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import shared.*


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

sealed class InviteProspectReceipt {
    object InviteFailed: InviteProspectReceipt()
    object InviteSuccess: InviteProspectReceipt()
    object InviteProspectNotFound: InviteProspectReceipt()
    object NoContactForProspect: InviteProspectReceipt()
    data class UnHandledError(val message: String): InviteProspectReceipt()
}

class InviteProspectUseCase(
    private val mail: MailNotifier,
    private val sms: SMSNotifier,
    private val invitations: InvitationService,
    private val flag: FlagService,
) {
    fun runFor(prospect: ProspectId): Mono<InviteProspectReceipt> {
        return invitations
            .generateInviteLinkfor(prospect)
            .flatMap { inviteLinkReceipt ->
                when (inviteLinkReceipt) {
                    is GenerateInviteLinkReceipt.Success ->
                        sendWelcomeEmail(prospect, inviteLinkReceipt.link).flatMap {sendMailReceipt ->
                               when (sendMailReceipt) {
                                   is SendMailReceipt.Success -> Mono.just(InviteProspectReceipt.InviteSuccess)
                                   is SendMailReceipt.ProspectNotFound -> Mono.just(InviteProspectReceipt.InviteProspectNotFound)
                                   is SendMailReceipt.ProspectHasNoEmail -> sendWelcomeSMS(prospect, inviteLinkReceipt.link).flatMap {smsReceipt ->
                                        when (smsReceipt) {
                                            is SendSMSReceipt.ProspectHasNoPhoneNumber -> flag.mark(prospect).map { InviteProspectReceipt.NoContactForProspect }
                                            else -> InviteProspectReceipt.UnHandledError("Something went horribly wrong!").toMono()
                                        }
                                   }
                                   is SendMailReceipt.Error -> InviteProspectReceipt.InviteFailed.toMono()
                               }
                        }
                    is GenerateInviteLinkReceipt.Error -> Mono.just(InviteProspectReceipt.InviteFailed)
                }
            }
    }

    private fun sendWelcomeEmail(prospect: ProspectId, inviteLink: InviteLink): Mono<SendMailReceipt> {
        val welcomeEmail = MailTemplate(subject = "Hello", body = "World $inviteLink")
        return mail.send(welcomeEmail, prospect)
    }

    private fun sendWelcomeSMS(prospect: ProspectId, inviteLink: InviteLink): Mono<SendSMSReceipt> {
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
    object Success : SendMailReceipt()
    object ProspectNotFound : SendMailReceipt()
    object ProspectHasNoEmail : SendMailReceipt()
    data class Error(val error: Throwable) : SendMailReceipt()
}

interface MailNotifier {
    fun send(mail: MailTemplate, to: ProspectId): Mono<SendMailReceipt>
}

sealed class SendSMSReceipt {
    object Success : SendSMSReceipt()
    object ProspectNotFound : SendSMSReceipt()
    object ProspectHasNoPhoneNumber : SendSMSReceipt()
    data class Error(val error: Throwable) : SendSMSReceipt()
}

interface SMSNotifier {
    fun send(sms: SMSTemplate, user: ProspectId): Mono<SendSMSReceipt>
}
