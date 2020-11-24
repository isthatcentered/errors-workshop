# Error handling Workshop

## Use case
Everytime we win a bidding contract, we want to onboard the prospect on our platform as soon and as painlessly as possible.
We accomplish that by sending them a welcome email containing a pre-filled registration link to our platform.

This invite is send automatically via our api when one of our operators marks the bidding contract as won.   

Currently, we send the invitation via email, unfortunately, some of our prospects never provided one.
When that's the case, we would like to fallback on a text message (Email should still be the primary delivery method). 

### Business Rules (Your todo list)
- [ ] If the prospect has no email, fallback to an SMS
- [ ] If sending the email fails, cancel the operation, we favour mail over text
- [ ] If sending the text fails, cancel the operation
- [ ] If the prospect has no contact (no email or sms), we should flag him for review
- [ ] Our api is not really user friendly, we always return a 500, some errors should have a friendly error message
     - [ ]Sometimes the operator enters a wrong Prospect id so the prospect is not found when sending the mail
     - [ ]If sending the email failed, we should prompt them to try again in a while
     - [ ]If the user has no contact they should be warned that the user has been flagged for review
     - [ ]500 is fine otherwise
- [ ] Also, the api should provide user friendly messages, if we have no contact for a prospect, 
     our operator should be warned.
- [ ] A prospect might now be blacklisted:
      - [ ] Would you program know about it ? 
      - [ ] What response will the caller get and does it feel right ?

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







// @todo: Todos:
- Mark throws sent by the various services
- Create a version pre typed to implement the feature via adt
- remove comments
- have solutions file
- have take away per section
- have hints per section
- Remove solutions
- initialize app to avoid highlighting
// NOTEs:
- we need a nested success action to show shortcircuiting 
- Per exrcise [ ] each wit it's questions ? <<- yup 
- Move the todolist & questions to the code ?
- we got rid of untyped errors but we lost shortcircuiting
- focusing on the combined notifier sms+email might be sufficient enough ? Combined with custom error response this could be simpler ?
- per section take away

### Take away  
si les pieces de base ne nous fournissent pas assez d'info, ne sont pas stables, dur de faire confiance à la composition de ces pieces.
we go in blind. We have success and yolo
smart constructor can fuck us without even knowing it
we can never be sure we caught everything we should 
(someone might add something we will never knwow)
(someone might remove a trhow, we will enver know either)
It's a strong stance taken by the thrower. Either it works as i expect or i crash your program. (in the case of try catch this is a havy burden)
No distinction between expected and panic errors 

## Level 2 - Algebraic Data Types (ADTs)
### Rules
- Overdo it
- blah should return an adt

pieces de base stables = facilité à implémenter, confiance++ rapidité++
type system will help you in case of change ? (non exhaustive matching)

add an error to show the type system will help you in case of change ? (non exhaustive matching)
