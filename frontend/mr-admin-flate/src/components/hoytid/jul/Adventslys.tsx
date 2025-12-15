import { CandleLightMeltSvg } from "./CandleLightMeltSvg";
import { CandleMeltSvg } from "./CandleMeltSvg";
import { CandleSvg } from "./CandleSvg";

export function Adventslys() {
  const dagensDato = new Date();
  const aar = new Date().getFullYear();
  const [forsteAdventDato, andreAdventDato, tredjeAdventDato, fjerdeAdventDato] =
    hentAdventsDatoer(aar);

  const harTent = {
    forsteLys: dagensDato >= forsteAdventDato,
    andreLys: dagensDato >= andreAdventDato,
    tredjeLys: dagensDato >= tredjeAdventDato,
    fjerdeLys: dagensDato >= fjerdeAdventDato,
  };

  const base = "flex h-full items-end overflow-hidden ml-auto mr-[10px] [&>svg]:translate-y-[2px]";

  const forste =
    harTent.forsteLys && !harTent.andreLys ? "[&>svg:nth-child(1)]:translate-y-[4px]" : "";

  const andre =
    harTent.andreLys && !harTent.tredjeLys
      ? "[&>svg:nth-child(1)]:translate-y-[8px] [&>svg:nth-child(2)]:translate-y-[4px]"
      : "";

  const tredje =
    harTent.tredjeLys && !harTent.fjerdeLys
      ? "[&>svg:nth-child(1)]:translate-y-[12px] [&>svg:nth-child(2)]:translate-y-[8px] [&>svg:nth-child(3)]:translate-y-[4px]"
      : "";

  const fjerde = harTent.fjerdeLys
    ? "[&>svg:nth-child(1)]:translate-y-[16px] [&>svg:nth-child(2)]:translate-y-[12px] [&>svg:nth-child(3)]:translate-y-[8px] [&>svg:nth-child(4)]:translate-y-[4px]"
    : "";

  return (
    <div className={`${base} ${forste} ${andre} ${tredje} ${fjerde}`}>
      {hentLysSvg(harTent.forsteLys, harTent.andreLys)}
      {hentLysSvg(harTent.andreLys, harTent.tredjeLys)}
      {hentLysSvg(harTent.tredjeLys, harTent.fjerdeLys)}
      {hentLysSvg(harTent.fjerdeLys, false)}
    </div>
  );
}

function hentLysSvg(erTent: boolean, harSmeltet: boolean) {
  if (harSmeltet) return <CandleMeltSvg />;
  if (erTent) return <CandleLightMeltSvg />;
  return <CandleSvg />;
}

function hentAdventsDatoer(aar: number) {
  const julemorgen = new Date(aar, 11, 25);
  const julemorgenDag = julemorgen.getDay(); // Day of the week for Christmas (0 = Sunday, 6 = Saturday)
  const fjerdeAdvent = new Date(julemorgen);
  fjerdeAdvent.setDate(julemorgen.getDate() - (julemorgenDag === 0 ? 7 : julemorgenDag)); // Adjust to the last Sunday before Christmas

  const tredjeAdvent = new Date(fjerdeAdvent);
  tredjeAdvent.setDate(fjerdeAdvent.getDate() - 7);

  const andreAdvent = new Date(tredjeAdvent);
  andreAdvent.setDate(tredjeAdvent.getDate() - 7);

  const forsteAdvent = new Date(andreAdvent);
  forsteAdvent.setDate(andreAdvent.getDate() - 7);

  return [forsteAdvent, andreAdvent, tredjeAdvent, fjerdeAdvent];
}
