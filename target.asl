

/* Crencas e regras iniciais */

// inicializacao das crencas (serao atualizadas depois)
larguraGrid(0).
alturaGrid(0).
ticksParaMover(10).
ticks(0).

/* Objetivos iniciais */



/* Planos */

// matriz(X,Y) - armazena a largura e a altura do grid 
+matriz(X,Y) : true
	<- 	-+larguraGrid(X); -+alturaGrid(Y);
      .print("ja sei o grid");
      +start.

// start - 
+start : true
	<- 	?pos(X,Y);
      .print("pos(",X,",",Y,")");
      !getNextMov.
  
+!getNextMov : ticks(C) & ticksParaMover(T) & C < T
	<- 	NovoC = C+1;
      -+ticks(NovoC);
      nop;
      !!getNextMov.
      
+!getNextMov : true
	<- 	nextMov;
      -+ticks(0);
      !!getNextMov.
