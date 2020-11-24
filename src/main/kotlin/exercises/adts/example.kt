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

		// 🛑
		// How could we make it total ?
		val prospect = ProspectId(body.data.prospectId)

		return inviteProspectUseCase.runFor(prospect)
			.flatMap { receipt ->
				when (receipt) {
					InviteProspectReceipt.Success -> respond(200, "All good")
					InviteProspectReceipt.ProspectHasNoContact -> respond(400, "No contacts found for prospect ${prospect.value}. Prospect has been marked for review.")
					InviteProspectReceipt.ProspectNotFound -> respond(400, "We could not find prospect ${prospect.value}. If this is the correct number please contact Jeanine")
					InviteProspectReceipt.CouldNotSendMail -> respond(500, "We found an email for this prospect but were not able to send it. Please try again in a few moments")
					is InviteProspectReceipt.Error -> respond(500, "Something went wrong")
				}
			}
			.onErrorResume { respond(500, "Something went wrong") }
	}

	// ... other controller routes (index, new, create, show, ...)
}

// 🛑 provide an empty one by default
sealed class InviteProspectReceipt {
	object Success : InviteProspectReceipt()
	object ProspectHasNoContact : InviteProspectReceipt()
	object ProspectNotFound : InviteProspectReceipt()
	object CouldNotSendMail : InviteProspectReceipt()
	data class Error(val error: Throwable) : InviteProspectReceipt()
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
						sendWelcomeEmail(prospect, inviteLinkReceipt.link)
							.flatMap { sendEmailReceipt ->
								// 🛑
								// How do you feel about having a clear checklist of things to deal with like this
								// But does it feel justified when you only care about the success case at this point ?
								when (sendEmailReceipt) {
									SendMail.Success -> Mono.just(InviteProspectReceipt.Success)
									SendMail.ProspectNotFound -> Mono.just(InviteProspectReceipt.ProspectNotFound)
									SendMail.ProspectHasNoEmail ->
										sendWelcomeSMS(prospect, inviteLinkReceipt.link)
											.flatMap { sendSmsReceipt ->
												when (sendSmsReceipt) {
													SendSMS.Success -> Mono.just(InviteProspectReceipt.Success)
													SendSMS.ProspectNotFound -> Mono.just(InviteProspectReceipt.ProspectNotFound)
													SendSMS.ProspectHasNoPhoneNumber ->
														flag.mark(prospect).map { flagProspectReceipt ->
															when (flagProspectReceipt) {
																// 🛑
																// Does it feel usefull to always have to define and deal with the general
																// "something broke and we don't knpw what" case ?
																MarkProspectReceipt.Success -> InviteProspectReceipt.ProspectHasNoContact
																is MarkProspectReceipt.Error -> InviteProspectReceipt.Error(flagProspectReceipt.error)
															}
														}
													is SendSMS.Error -> Mono.just(InviteProspectReceipt.Error(sendSmsReceipt.error))
												}
											}
									is SendMail.Error -> Mono.just(InviteProspectReceipt.CouldNotSendMail)
								}
							}
					// 🛑
					// - Note how the compiler won't let you access the link here, this is not the success case
					is GenerateInviteLinkReceipt.Error ->
						Mono.just(InviteProspectReceipt.Error(inviteLinkReceipt.error))
				}
			}
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

// @todo: this 2 could implement a single generic interface.
//  That would be interesting to see how you can have distinct SMS & Mail ProspectHasNo{CONTACT} error in this case
sealed class SendSMSReceipt {
	object Success : SendSMS()
	object ProspectNotFound : SendSMS()
	object ProspectHasNoPhoneNumber : SendSMS()
	data class Error(val error: Throwable) : SendSMS()
}

interface SMSNotifier {
	fun send(sms: SMSTemplate, user: ProspectId): Mono<SendSMS>
}
