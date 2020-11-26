package exercises.adts

import exercises.adts.InviteProspectReceipt.*
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
            .flatMap { receipt ->
                when (receipt) {
                    InviteSuccess -> respond(200, "All good")
                    InviteFailed -> respond(500, "Can not generate invitation link")
                    InviteProspectNotFound -> respond(400, "Can not found prospect")
                    NoContactForProspect -> respond(500, "No contact for selected prospect")
                    BlacklistedProspect -> respond(400, "Prospect has been banned")
                    is UnHandledError -> respond(500, receipt.message)
                }
            }
            .onErrorResume { respond(500, "Something went wrong: ${it.message}") }
    }

    // ... other controller routes (index, new, create, show, ...)
}

sealed class InviteProspectReceipt {
    object InviteFailed: InviteProspectReceipt()
    object InviteSuccess: InviteProspectReceipt()
    object InviteProspectNotFound: InviteProspectReceipt()
    object NoContactForProspect: InviteProspectReceipt()
    object BlacklistedProspect: InviteProspectReceipt()
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
                                   is SendMailReceipt.Success -> Mono.just(InviteSuccess)
                                   is SendMailReceipt.ProspectNotFound -> Mono.just(InviteProspectNotFound)
                                   is SendMailReceipt.ProspectHasNoEmail -> sendWelcomeSMS(prospect, inviteLinkReceipt.link).flatMap {smsReceipt ->
                                        when (smsReceipt) {
                                            is SendSMSReceipt.ProspectHasNoPhoneNumber -> flag.mark(prospect).map { NoContactForProspect }
                                            else -> UnHandledError("Something went horribly wrong!").toMono()
                                        }
                                   }
                                   is SendMailReceipt.Error -> InviteFailed.toMono()
                               }
                        }
                    is GenerateInviteLinkReceipt.Error -> Mono.just(InviteFailed)
                    GenerateInviteLinkReceipt.BlacklistedProspect -> Mono.just(BlacklistedProspect)
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
    object BlacklistedProspect : GenerateInviteLinkReceipt()
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
