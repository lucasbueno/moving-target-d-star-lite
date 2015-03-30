

/* Crencas e regras iniciais */

// inicializacaoo das crencas (serao atualizadas depois)
larguraGrid(0).
alturaGrid(0).

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

+!getNextMov : pos(X,Y) & target(X,Y)
	<- 	.print("-- Alvo alcancado! --").			
  
+!getNextMov : novoPlano(L) & LC = .length(L) & LC > 0
	<- 	L = [P|_];
      !irPara(P);
      !!getNextMov.	

+!getNextMov : true
	<- !!getNextMov.
  
+!irPara(P) : true
	<-	.print(P);
		P.
