/* Bonificación recreativa y local; no equivale a puntuación antifraude. */
import {readResearch,researchBonus} from './research-log.js';
export const DONUT_KEY='springfield:donut-clue:v2';
export function hintFromEvidence(storage){
 try{
  return storage?.getItem(DONUT_KEY)==='yes'||Boolean(researchBonus(readResearch(storage)));
 }catch{return false;}
}
