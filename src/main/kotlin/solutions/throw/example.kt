package solutions.`throw`

import solutions.`throw`.CouldNotSendEmailException
import solutions.`throw`.ProspectHasNoPhoneNumberException
import solutions.`throw`.ProspectNotFoundException
import framework.RequestBody
import framework.ServerResponse
import framework.respond
import reactor.core.publisher.Mono
import shared.*


class InviteController(private val inviteProspectUseCase: InviteProspectUseCase) {
	fun create(body: RequestBody<InviteProspectParams>): Mono<ServerResponse<String>> {
		body.validate()

		// 🛑
		// This looks harmless right ?
		val prospect = ProspectId(body.data.prospectId)

		return inviteProspectUseCase.runFor(prospect)
			.flatMap { respond(200, "Ok") }
			.onErrorResume { err ->
				when (err) {
					is ProspectNotFoundException -> respond(400, "We could not find prospect ${err.id}. If this is the correct number please contact Jeanine")
					// 🛑
					// This requires knowledge of the implementation, we only have this error if we found no email, it probably doesn't belong here
					// The fact that we translate it to a totally different message is a hint
					is ProspectHasNoPhoneNumberException -> respond(400, "No contacts found for prospect ${err.id}. Prospect has been marked for review.")
					is CouldNotSendEmailException -> respond(500, "We found an email for this prospect but were not able to send it. Please try again in a few moments")
					// 🛑
					// We find a separation between expected exceptions that require a custom error message
					// and an unexpected system error that we can only convert to a 500
					else -> respond(500, "Something went wrong")
				}
			}
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
					.onErrorResume { sendWelcomeSMS(prospect, inviteLink) }
			}
			// 🛑
			// While we lose type safety on the error case, the shortcircuiting behavior of try catch is very practicle
			.onErrorResume { err ->
				when (err) {
					// 🛑
					// What happens here if the sms notifier changes the type of error it throws ?
					is ProspectHasNoPhoneNumberException -> flag.mark(prospect).flatMap { Mono.error(err) }
					else -> Mono.error(err)
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
	fun send(mail: MailTemplate, to: ProspectId): Mono<Unit>
}

interface SMSNotifier {
	fun send(sms: SMSTemplate, user: ProspectId): Mono<Unit>
}
