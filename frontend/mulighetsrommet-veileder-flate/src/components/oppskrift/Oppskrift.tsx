import { useOppskrifter } from "@/api/queries/useOppskrifter";
import { Alert, Button } from "@navikt/ds-react";
import { useEffect, useRef } from "react";
import { APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import { RedaksjoneltInnhold } from "../RedaksjoneltInnhold";

interface Props {
  oppskriftId: string;
  tiltakstypeId: string;
  setOppskriftId: (id: string | undefined) => void;
}

export function Oppskrift({ oppskriftId, tiltakstypeId, setOppskriftId }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const { data: oppskrifter } = useOppskrifter(tiltakstypeId);

  useEffect(() => {
    if (ref.current) {
      ref.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [oppskriftId]);

  if (!oppskrifter) return null;

  const oppskrift = oppskrifter.data.find((oppskrift) => oppskrift._id === oppskriftId);

  if (!oppskrift) {
    return <Alert variant="warning">Vi kunne dessverre ikke finne oppskriften</Alert>;
  }

  function navigateViaShadowDomToElement(elementId: string) {
    // Siden vi bruker en shadow-dom når vi bygger appen som en web-component så fungerer ikke
    // vanlig navigering med anchor tags og id'er så vi må bruke querySelector for å
    // hente ut elementet enten via shadow-dom (via ?.shadowRoot) eller direkte for så å scrolle til elementet
    const shadowDom = document.querySelector(APPLICATION_WEB_COMPONENT_NAME)?.shadowRoot;
    if (shadowDom) {
      // Dette skjer når vi bygger appen som en web-component
      shadowDom.querySelector(elementId)?.scrollIntoView({ behavior: "smooth" });
    } else {
      // Dette skjer ved lokal kjøring av appen
      document.querySelector(elementId)?.scrollIntoView({ behavior: "smooth" });
    }
  }

  return (
    <>
      <Button variant="tertiary-neutral" onClick={() => setOppskriftId(undefined)}>
        Lukk oppskriften
      </Button>
      <div className="flex bg-white p-4">
        <aside className="mx-4">
          <nav>
            <ol className="m-0 p-0">
              {oppskrift.steg.map((s, index) => {
                return (
                  <li key={s.navn} className="my-4 mx-0">
                    <button
                      className="bg-none border-none underline text-left hover:cursor-pointer"
                      title={`Naviger til steg: ${index + 1}`}
                      aria-label={`Naviger til steg: ${index + 1}`}
                      onClick={() => navigateViaShadowDomToElement(`#steg-${index + 1}`)}
                    >
                      {s.navn}
                    </button>
                  </li>
                );
              })}
            </ol>
          </nav>
        </aside>
        <section ref={ref} className="overflow-y-scroll max-h-[50rem]">
          <h3>{oppskrift.navn}</h3>
          <p>{oppskrift.beskrivelse}</p>
          {oppskrift.steg.map((st, index) => {
            return (
              <div
                key={st.navn}
                className="bg-gray-50 p-4 my-4 mx-0 rounded-[0.2rem] [&_img]:w-full"
              >
                <h4 id={`steg-${index + 1}`}>{`${index + 1}. ${st.navn}`}</h4>
                <RedaksjoneltInnhold value={st.innhold} />
              </div>
            );
          })}
        </section>
      </div>
    </>
  );
}
