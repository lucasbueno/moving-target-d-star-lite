

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
      
+target(X,Y) : true
  <-  search.

  		
//+!getNextMov : plano(Xp,Yp,L) & target(Xt,Yt) & (Xp \== Xt | Yp \== Yt)  
//	<- 	search;
//      !!getNextMov.
  
+!getNextMov : plano(L) & LC = .length(L) & LC > 0
	<- 	L = [P|_];
      !irPara(P);
      !!getNextMov.

+!getNextMov : plano(L) & LC = .length(L) & LC == 0
	<- 	.print("-- Alvo alcancado! --").				

+!getNextMov : true
	<- 	!!getNextMov.

+!irPara(P) : P = moveTo(X,Y) & pos(X,Y)
	<-	?plano(L);
      L = [P|T];
      -+plano(T);
      LC = .length(T);
      .print("plano(",LC,") = ", T).
		
+!irPara(P) : true
	<-	P.

+novoPlano(L) : LC = .length(L)
	<- 	.print("plano(",LC,") = ", L);
		  -+plano(L).
