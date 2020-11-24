## Themes
### Intro
- An app is a composition of small pieces, if those small pieces aren't stable, the whole won't be either
- This "solidity" of an app is perceived through resilience and error handling
- Workshop introduces a different way to deal with errors
- We'll discuss pros/cons for each
- Starting w/ "classic" way to get a feel for where we are

### Level 1 - Throwing
#### Intro
#### Takeway (pros/cons)
// Cons
- Throw is 100% invisible, won't know if you don't check
- How can you take the right decision if you go in blind ?
- Lack of confidence when the rules start piling on
- Hard to know what might happen at the top level
// Pros

### Level 2 - Algebraic Data Types (ADTs)
#### Intro
- Underlying problem ? Partial vs total fns
- Adts to rescue

#### Takeway (pros/cons)
// Cons
// Pros

### Level 3 - Either
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
