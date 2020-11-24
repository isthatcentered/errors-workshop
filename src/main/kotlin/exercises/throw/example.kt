package exercises.`throw`

import framework.RequestBody
import framework.ServerResponse
import framework.respond
import reactor.core.publisher.Mono
import shared.*


class InviteController(private val inviteProspectUseCase: InviteProspectUseCase) {
    fun create(body: RequestBody<InviteProspectParams>): Mono<ServerResponse<String>> {
        body.validate()

        val prospect = ProspectId(body.data.prospectId)

        return inviteProspectUseCase.runFor(prospect)
            .flatMap { respond(200, "Ok") }
            .onErrorResume { respond(500, "Something went wrong") }
    }

    // ... other controller routes (index, new, create, show, ...)
}


class InviteProspectUseCase(
    private val mail: MailNotifier,
    private val sms: SMSNotifier,
    private val invitations: InvitationService,
    private val flag: FlagService,
) {
    fun runFor(prospect: ProspectId): Mono<Unit> {
        return invitations
            .generateInviteLinkfor(prospect)
            .flatMap { inviteLink ->
                sendWelcomeEmail(prospect, inviteLink)
                    .onErrorResume { err ->
                        when (err) {
                            is ProspectHasNoMailException -> sendWelcomeSMS(prospect, inviteLink)
                            is ProspectNotFoundException -> Mono.error(err)
                            is CouldNotSendEmailException -> Mono.error(err)
                            else -> Mono.error(err)
                        }
                    }
            }
    }

    private fun sendWelcomeEmail(prospect: ProspectId, inviteLink: InviteLink): Mono<Unit> {
        val welcomeEmail = MailTemplate(subject = "Hello", body = "World $inviteLink")
        return mail.send(welcomeEmail, prospect)
    }

    private fun sendWelcomeSMS(prospect: ProspectId, inviteLink: InviteLink): Mono<Unit> {
        val text = SMSTemplate("Hello $inviteLink")
        return sms.send(text, prospect)
    }
}


interface InvitationService {
    fun generateInviteLinkfor(propspect: ProspectId): Mono<InviteLink>
}

interface FlagService {
    fun mark(prospect: ProspectId): Mono<Unit>
}

interface MailNotifier {

    /**
     * throws ProspectNotFoundException
     * throws ProspectHasNoEmailException
     * throws
     */
    fun send(mail: MailTemplate, to: ProspectId): Mono<Unit>
}

interface SMSNotifier {
    fun send(sms: SMSTemplate, user: ProspectId): Mono<Unit>
}
