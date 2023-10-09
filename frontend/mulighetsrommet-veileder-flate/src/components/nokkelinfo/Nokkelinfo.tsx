import { BodyShort, Heading, HelpText } from "@navikt/ds-react";
import styles from "./Nokkelinfo.module.scss";

export interface NokkelinfoProps {
  nokkelinfoKomponenter: NokkelinfoElement[];
  uuTitle?: string;
}

export interface NokkelinfoElement {
  _id: string;
  tittel?: string;
  innhold?: string | React.JSX.Element;
  hjelpetekst?: string;
}

const Nokkelinfo = ({ nokkelinfoKomponenter, uuTitle, ...rest }: NokkelinfoProps) => {
  return (
    <div className={styles.container} {...rest}>
      {nokkelinfoKomponenter.map((nokkelinfo, index) => {
        return (
          <div className={styles.nokkelinfo} key={index}>
            <div className={styles.content}>
              {typeof nokkelinfo.innhold === "string" ? (
                <BodyShort className={styles.tekst}>{nokkelinfo.innhold}</BodyShort>
              ) : (
                <div className={styles.tekst}>{nokkelinfo.innhold}</div>
              )}

              {nokkelinfo.hjelpetekst && (
                <HelpText title={uuTitle} placement="right" style={{ maxWidth: "400px" }}>
                  {nokkelinfo.hjelpetekst}
                </HelpText>
              )}
            </div>
            <Heading className={styles.heading} size="xsmall" level="2">
              {nokkelinfo.tittel}
            </Heading>
          </div>
        );
      })}
    </div>
  );
};

export default Nokkelinfo;
