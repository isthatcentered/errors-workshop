# Error handling Workshop

## Use case
Everytime we win a bidding contract, we want to onboard the prospect on our platform as soon and as painlessly as possible.
We accomplish that by sending them a welcome email containing a pre-filled registration link to our platform.

This invite is send automatically via our api when one of our operators marks the bidding contract as won.   

Currently, we send the invitation via email, unfortunately, some of our prospects never provided one.
When that's the case, we would like to fallback on a text message (Email should still be the primary delivery method). 

### Business Rules (Your todo list)
- [*] If the prospect has no email, fallback to an SMS
- [*] If sending the email fails, cancel the operation, we favour mail over text
- [*] If sending the text fails, cancel the operation
- [*] If the prospect has no contact (no email or sms), we should flag him for review
- [*] Our api is not really user friendly, we always return a 500, some errors should have a friendly error message
     - [*]Sometimes the operator enters a wrong Prospect id so the prospect is not found when sending the mail
     - [*]If sending the email failed, we should prompt them to try again in a while
     - [*]If the user has no contact they should be warned that the user has been flagged for review
     - [*]500 is fine otherwise
- [*] A prospect might now be blacklisted:
      - [*] Would you program know about it ? 
      - [*] What response will the caller get and does it feel right ?

## How to
Except a few unimportant shared classes, each file contains the import collaborators for the example. 

## Level 1 - Throwing
- What did you learn ? 
  Hints, what did you think of: 
  - 
  -
  -
  -
- What are the benefits of this approach ?
- What are the disadvantages of this approach ?

## Level 2 - Algebraic Data Types (ADTs)
- What did you learn ? 
  Hints, what did you think of: 
  - 
  -
  -
  -
- What are the benefits of this approach ?
- What are the disadvantages of this approach ?

## Level 3 - Either
## Bonus - Implement Either






