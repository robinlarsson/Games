## Some decisions made

- Player count: Each game should specify how many players can play the game, with a lower limit of 1 and then up to practical infinity. All board games you see in real life will have a specification that says how many players can play the game.
- Setting up a game: To setup a game the game basically needs to know how many players should play, and also potentially some additional configuration for the game (difficulty level, which rules to use, and so on)
- Inviting players: While inviting players they should be able to choose some player-specific stuff, such as "Be on this team" or "Use this deck to play" or "Use this character". This is done after setting up the general game-specific settings.
- Winning/Losing a game: Everything is just "Eliminations". A player can be eliminated and be considered either as a winner or loser (or draw). Multiple players can reach 1st place (or any place). Eliminating a player basically means "You can't play anymore", so it can also be used to determine the result of a one-player game.
- Viewing a game: Because of Vue.js, and to easily support observing games, and for potentially other reasons, the game state is sent as JSON and automatically handled by Vue.js to setup the current game state. So implementing support for a new game in frontend is as easy as binding state to components - which is the very basics of Vue.js.
- Making moves in a game - Backend: As games vary and as the complexity of the type of action you perform varies, there are a few different ways to specify an action. For example, "End Turn" is a very simple action - it does not have any parameters. "Choose a number" has a parameter - The number. "Click on a tile in the grid" has a parameter - the tile that is clicked. Then there's more complex ones, like giving a clue to a player in the game of Hanabi: You need to both choose a player, choose if you want to give a clue for color or number, and then choose the value for the color/number. But breaking the give clue action down into the three smaller steps (player, color/number, value) makes it possible to handle this action and at every step show the player which possible options exist.
- Making moves in a game - Frontend: In order to highlight possible actions, each action (or part of an action) maps to a string which allows for easy lookup to determine if a specific action is allowed. Then clicking that corresponding component should trigger the corresponding action.
- Frontend re-use: Because of how the "viewing a game" and "Making moves in a game - Frontend" is handled, frontend uses the same code for both playing a game locally - without connecting to a server - and online - with connecting to a server.
- Saving game in database: As a game can contain random events (rolling a die, shuffling cards) the game code needs to tell the server "I did a random thing and this was the result", and it also needs to handle the server telling the game code "You should do this action and use this state for the random things" (for loading or replaying games).
- Database design: Amazon DynamoDB is used for games that are in progress, and to store the moves for each game. But PostgresSQL is used for statistics, as it allows more complex queries that would be cumbersome to make in DynamoDB ("Get the summary of which players I have played Tic-Tac-Toe against and sort by the highest win percentage" can be made in PostgresSQL and while it's probably possible to make it in DynamoDB as well, I concluded that it would be cumbersome to design that)