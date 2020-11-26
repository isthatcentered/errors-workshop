package exercises.either

import arrow.core.Either
import arrow.core.flatMap
import framework.RequestBody
import framework.ServerResponse
import framework.respond
import reactor.core.publisher.Mono
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

// crates an invite in db
// generates a 1 time login link
generateInvite(prospectId) // no user not found for this, this is pure sync fn
sendwelcome
    contacts.for(propspectId)
        flags.needsReview
    notify(contacts, mail, notifyPolicy)
flags.markAsPending

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
    fun runFor(prospect: ProspectId): Either<InviteProspectError, Unit> {
        val computation = invitations
            .generateInviteLinkfor(prospect)
//            .task()
//            .mapLeft(InviteProspectError::UnexpectedError)
//            .flatMap { inviteLink -> sendWelcome(prospect, inviteLink) }

        return TODO()
    }

//    private fun sendWelcome(prospect: ProspectId, inviteLink: InviteLink): Either<InviteProspectError, Unit> {
//        return sendWelcomeEmail(prospect, inviteLink)
//            .flatMapLeft {
//                when (it) {
//                    SendMailError.ProspectNotFound -> InviteProspectError.ProspectNotFound.left()
//                    SendMailError.ProspectHasNoEmail ->
//                        sendWelcomeSMS(prospect, inviteLink)
//                            .flatMapLeft {
//                                when (it) {
//                                    SendSMSError.ProspectNotFound -> InviteProspectError.ProspectNotFound.left()
//                                    SendSMSError.ProspectHasNoPhoneNumber ->
//                                        flag.mark(prospect)
//                                            .shortcircuit<InviteProspectError>(InviteProspectError.ProspectHasNoContact)
//                                }
//                            }
//                }
//            }
//    }

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
    fun generateInviteLinkfor(propspect: ProspectId): Mono<InviteLink>
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
