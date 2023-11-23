import { Alert, GuidePanel } from "@navikt/ds-react";
import { PortableText } from "@portabletext/react";
import { useEffect, useRef } from "react";
import { Link, useParams } from "react-router-dom";
import { useOppskrifter } from "../../core/api/queries/useOppskrifter";
import styles from "./Oppskrift.module.scss";
import { APPLICATION_WEB_COMPONENT_NAME } from "../../constants";

interface ImageProp {
  value: { asset: { url: string }; altText: string };
}

interface TipsProps {
  value: { innhold: Record<any, any> };
}

interface AlertMessageProps {
  value: { variant: "info" | "warning" | "error"; innhold: Record<any, any> };
}

const oppskriftPortableText = {
  types: {
    image: ({ value }: ImageProp) => {
      return (
        <a href={value.asset.url}>
          <img src={value.asset.url} alt={value.altText} />
        </a>
      );
    },
    tips: ({ value }: TipsProps) => {
      return (
        <GuidePanel>
          <PortableText value={value.innhold} components={oppskriftPortableText} />
        </GuidePanel>
      );
    },
    alertMessage: ({ value }: AlertMessageProps) => {
      return (
        <Alert style={{ margin: "1rem 0" }} variant={value.variant}>
          <PortableText value={value.innhold} components={oppskriftPortableText} />
        </Alert>
      );
    },
  },
};

export function Oppskrift() {
  const { oppskriftId, tiltakstypeId } = useParams();
  const ref = useRef<HTMLDivElement>(null);
  const { data: oppskrifter } = useOppskrifter(tiltakstypeId);

  useEffect(() => {
    if (ref?.current) {
      ref?.current?.scrollIntoView({ behavior: "smooth" });
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
      // Dette skjer lokal kjøring av appen
      document.querySelector(elementId)?.scrollIntoView({ behavior: "smooth" });
    }
  }

  return (
    <>
      <Link to="..">Lukk oppskriften</Link>
      <div className={styles.container}>
        <aside className={styles.navigering}>
          <nav>
            <ol>
              {oppskrift.steg.map((s, index) => {
                return (
                  <li key={s.navn}>
                    <a
                      onClick={() => navigateViaShadowDomToElement(`#steg-${index + 1}`)}
                      style={{ cursor: "pointer" }}
                    >
                      {s.navn}
                    </a>
                  </li>
                );
              })}
            </ol>
          </nav>
        </aside>
        <section ref={ref} className={styles.oppskrifter}>
          <h3>{oppskrift.navn}</h3>
          <p>{oppskrift.beskrivelse}</p>
          {oppskrift.steg.map((st, index) => {
            return (
              <div key={st.navn} className={styles.steg}>
                <h4 id={`steg-${index + 1}`}>{`${index + 1}. ${st.navn}`}</h4>
                <PortableText value={st.innhold} components={oppskriftPortableText} />
              </div>
            );
          })}
        </section>
      </div>
    </>
  );
}
