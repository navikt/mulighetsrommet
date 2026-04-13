import { Alert, BodyLong, Box, Heading, List, VStack } from "@navikt/ds-react";
import { LokalInformasjonContainer, PortableText } from "@mr/frontend-common";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { Faneinnhold, FaneinnholdLenke, TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";

interface RedaksjoneltInnholdProps {
  tiltakstype: TiltakstypeDto;
  beskrivelse: string | null;
  faneinnhold: Faneinnhold | null;
}

export function RedaksjoneltInnhold(props: RedaksjoneltInnholdProps) {
  const { tiltakstype, beskrivelse, faneinnhold } = props;

  return (
    <RedaksjoneltInnholdContainer>
      {tiltakstype.beskrivelse && (
        <>
          <Heading size="medium">Generell informasjon</Heading>
          <BodyLong size="large" style={{ whiteSpace: "pre-wrap" }}>
            {tiltakstype.beskrivelse}
          </BodyLong>
        </>
      )}
      {beskrivelse && (
        <LokalInformasjonContainer>
          <BodyLong style={{ whiteSpace: "pre-wrap" }} textColor="subtle" size="medium">
            {beskrivelse}
          </BodyLong>
        </LokalInformasjonContainer>
      )}
      {someValuesExists([
        faneinnhold?.forHvem,
        faneinnhold?.forHvemInfoboks,
        tiltakstype.faneinnhold?.forHvem,
        tiltakstype.faneinnhold?.forHvemInfoboks,
      ]) ? (
        <>
          <Heading size="medium">For hvem</Heading>
          <DetaljerFane
            gjennomforing={faneinnhold?.forHvem}
            gjennomforingAlert={faneinnhold?.forHvemInfoboks}
            tiltakstype={tiltakstype.faneinnhold?.forHvem}
            tiltakstypeAlert={tiltakstype.faneinnhold?.forHvemInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([
        faneinnhold?.detaljerOgInnhold,
        faneinnhold?.detaljerOgInnholdInfoboks,
        tiltakstype.faneinnhold?.detaljerOgInnhold,
        tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks,
      ]) ? (
        <>
          <Heading size="medium">Detaljer og innhold</Heading>
          <DetaljerFane
            gjennomforing={faneinnhold?.detaljerOgInnhold}
            gjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
            tiltakstype={tiltakstype.faneinnhold?.detaljerOgInnhold}
            tiltakstypeAlert={tiltakstype.faneinnhold?.detaljerOgInnholdInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([
        faneinnhold?.pameldingOgVarighet,
        faneinnhold?.pameldingOgVarighetInfoboks,
        tiltakstype.faneinnhold?.pameldingOgVarighet,
        tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks,
      ]) ? (
        <>
          <Heading size="medium">Påmelding og varighet</Heading>
          <DetaljerFane
            gjennomforing={faneinnhold?.pameldingOgVarighet}
            gjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
            tiltakstype={tiltakstype.faneinnhold?.pameldingOgVarighet}
            tiltakstypeAlert={tiltakstype.faneinnhold?.pameldingOgVarighetInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([faneinnhold?.kontaktinfo, faneinnhold?.kontaktinfoInfoboks]) ? (
        <>
          <Heading size="medium">Kontaktinfo</Heading>
          <DetaljerFane
            gjennomforing={faneinnhold?.kontaktinfo}
            gjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
          />
        </>
      ) : null}

      {someValuesExists([faneinnhold?.lenker]) ? (
        <div className="prose">
          <Heading size="medium">Lenker</Heading>
          <RedaksjoneltInnholdLenker lenker={faneinnhold?.lenker || []} />
        </div>
      ) : null}

      {someValuesExists([faneinnhold?.delMedBruker, tiltakstype.faneinnhold?.delMedBruker]) ? (
        <>
          <Heading size="medium">Del med bruker</Heading>
          <BodyLong as="div" size="small" className="prose">
            {faneinnhold?.delMedBruker ?? tiltakstype.faneinnhold?.delMedBruker}
          </BodyLong>
        </>
      ) : null}
    </RedaksjoneltInnholdContainer>
  );
}

function someValuesExists(params: any[]): boolean {
  return params.some((p) => !!p);
}

interface DetaljerFaneProps {
  gjennomforingAlert?: string | null;
  tiltakstypeAlert?: string | null;
  gjennomforing?: any;
  tiltakstype?: any;
}

const DetaljerFane = ({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) => {
  if (!gjennomforingAlert && !tiltakstypeAlert && !gjennomforing && !tiltakstype) {
    return <></>;
  }

  return (
    <div>
      {tiltakstype && (
        <>
          {tiltakstypeAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {tiltakstypeAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={tiltakstype} />
          </BodyLong>
        </>
      )}
      {(gjennomforing || gjennomforingAlert) && (
        <LokalInformasjonContainer>
          <Heading level="2" size="small" spacing className="mt-0">
            Lokal Informasjon
          </Heading>
          {gjennomforingAlert && (
            <Alert style={{ whiteSpace: "pre-wrap" }} variant="info">
              {gjennomforingAlert}
            </Alert>
          )}
          <BodyLong as="div" size="small">
            <PortableText value={gjennomforing} />
          </BodyLong>
        </LokalInformasjonContainer>
      )}
    </div>
  );
};

export function RedaksjoneltInnholdLenker({ lenker }: { lenker: FaneinnholdLenke[] }) {
  return (
    <Box marginBlock="space-16" asChild>
      <List data-aksel-migrated-v8 as="ul">
        {lenker.map((lenke, index) => (
          <List.Item key={index} className="break-words">
            <VStack className="max-w-full overflow-hidden">
              <LenkeComponent
                to={lenke.lenke}
                target={lenke.apneINyFane ? "_blank" : "_self"}
                rel={lenke.apneINyFane ? "noopener noreferrer" : undefined}
                isExternal={lenke.apneINyFane}
                className="break-all"
              >
                {lenke.lenkenavn} ({lenke.lenke})
              </LenkeComponent>
              {lenke.visKunForVeileder ? <small>Vises kun i Modia</small> : null}
            </VStack>
          </List.Item>
        ))}
      </List>
    </Box>
  );
}
