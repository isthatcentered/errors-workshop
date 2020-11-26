package exercises.either

import arrow.core.Either
import arrow.core.extensions.either.bifunctor.leftWiden
import arrow.core.flatMap
import arrow.core.handleErrorWith
import framework.RequestBody
import framework.ServerResponse
import shared.*

class InviteController(private val inviteProspectUseCase: InviteProspectUseCase) {
    fun create(body: RequestBody<InviteProspectParams>): ServerResponse<String> {
        body.validate()

        val prospect = ProspectId(body.data.prospectId)

        return inviteProspectUseCase.runFor(prospect)
            .fold(
                { err -> ServerResponse(500, "Something went wrong") },
                { receipt -> ServerResponse(200, "All good") }
            )
    }

    // ... other controller routes (index, new, create, show, ...)
}

sealed class InviteProspectError {
    object ProspectHasNoContact : InviteProspectError()
    object ProspectNotFound : InviteProspectError()
    object CouldNotSendMail : InviteProspectError()
    data class UnexpectedError(val error: Throwable) : InviteProspectError()
}

class InviteProspectUseCase(
    private val mail: MailNotifier,
    private val sms: SMSNotifier,
    private val invitations: InvitationService,
    private val flag: FlagService,
) {
    fun runFor(prospect: ProspectId): Either<InviteProspectError, String> {
//        return invitations.generateInviteLinkfor(prospect).bimap({
//            when (it) {
//                InvitationError.ProspectNotFound -> InviteProspectError.ProspectNotFound
//                InvitationError.ProspectBlacklisted -> InviteProspectError.ProspectNotFound
//            }
//        },{
//            sendWelcomeEmail(prospect, it)
//        })
        return invitations.generateInviteLinkfor(prospect)
                .mapLeft {
                    when (it) {
                        InvitationError.ProspectNotFound -> InviteProspectError.ProspectNotFound
                        InvitationError.ProspectBlacklisted -> InviteProspectError.ProspectNotFound
                    }
                }
                .flatMap { link ->
                    sendWelcomeEmail(prospect, link).handleErrorWith { sendMailError ->
                        when (sendMailError) {
                            SendMailError.ProspectNotFound -> InviteProspectError.ProspectNotFound
                            SendMailError.ProspectHasNoEmail -> sendWelcomeSMS(prospect, link).mapLeft { sendSmsError ->
                                when(sendSmsError) {
                                    SendSMSError.ProspectNotFound -> InviteProspectError.ProspectNotFound
                                    SendSMSError.ProspectHasNoPhoneNumber -> InviteProspectError.ProspectHasNoContact
                                }
                            }
                        }
                    }
                }
                .map { "Invitation sent." }
    }

    private fun sendWelcomeEmail(prospect: ProspectId, inviteLink: InviteLink): Either<SendMailError, Unit> {
        val welcomeEmail = MailTemplate(subject = "Hello", body = "World $inviteLink")
        return mail.send(welcomeEmail, prospect)
    }

    private fun sendWelcomeSMS(prospect: ProspectId, inviteLink: InviteLink): Either<SendSMSError, Unit> {
        val text = SMSTemplate("Hello $inviteLink")
        return sms.send(text, prospect)
    }
}

interface InvitationService {
    // 🛑 actually the guy might not exist or something
    fun generateInviteLinkfor(propspect: ProspectId): Either<InvitationError, InviteLink>
}

sealed class InvitationError {
    object ProspectNotFound : InvitationError()
    object ProspectBlacklisted : InvitationError()
}


// 🛑 Is an either justified here ?
interface FlagService {
    fun mark(prospect: ProspectId): Either<Unit, Unit>
}


sealed class SendMailError {
    object ProspectNotFound : SendMailError()
    object ProspectHasNoEmail : SendMailError()
}

interface MailNotifier {
    fun send(mail: MailTemplate, to: ProspectId): Either<SendMailError, Unit>
}

sealed class SendSMSError {
    // 🛑 No need to add the success case
    object ProspectNotFound : SendSMSError()
    object ProspectHasNoPhoneNumber : SendSMSError()
    // 🛑 Why no general error type ?
}

interface SMSNotifier {
    fun send(sms: SMSTemplate, user: ProspectId): Either<SendSMSError, Unit>
}
