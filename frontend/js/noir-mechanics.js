/** Reglas puras de inspeccion y desenlaces de Springfield Files. */
export function inspectionCost(completed,free=2){
 if(!Number.isSafeInteger(completed)||completed<0)return 0;
 return completed<free?0:6;
}
export function caseGrade(health){
 const value=Number(health);
 if(!Number.isFinite(value))return {title:'Informe sin validar',message:'La integridad no pudo calcularse.'};
 if(value>=90)return {title:'Inspector del mes',message:'El Ayuntamiento no sabe por que has investigado tan bien. Sospecha de una averia administrativa.'};
 if(value>=60)return {title:'Caso cerrado, con reservas',message:'Las pruebas cuadran. La seccion de grapas exige una nueva firma para archivar.'};
 return {title:'Archivo rescatado por los pelos',message:'La investigacion llego a buen puerto, aunque Burns descuenta de tu sueldo cada inspeccion extra.'};
}

/** Descuenta integridad sin valores negativos, NaN o mayores que 100. */
export function applyIntegrityCost(current, cost){
 if(!Number.isFinite(current)||!Number.isFinite(cost))return 0;
 return Math.max(0,Math.min(100,Math.round(current))-Math.max(0,Math.round(cost)));
}
export function investigationFailed(health){return !Number.isFinite(health)||health<=0;}
