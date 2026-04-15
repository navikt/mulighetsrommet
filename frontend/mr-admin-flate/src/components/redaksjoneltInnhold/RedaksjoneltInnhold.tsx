import { Alert, BodyLong, Box, Heading, HStack, List, VStack } from "@navikt/ds-react";
import { LokalInformasjonContainer, PortableText } from "@mr/frontend-common";
import { RedaksjoneltInnholdContainer } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdContainer";
import { RedaksjoneltInnholdTabs } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdTabs";
import { Faneinnhold, FaneinnholdLenke, TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { Lenke as LenkeComponent } from "@mr/frontend-common/components/lenke/Lenke";

interface Props {
  tiltakstype: TiltakstypeDto;
  beskrivelse: string | null;
  faneinnhold: Faneinnhold | null;
}

export function RedaksjoneltInnhold({ tiltakstype, beskrivelse, faneinnhold }: Props) {
  const veilederinfo = tiltakstype.veilederinfo;

  const lenker = [...(veilederinfo.faneinnhold?.lenker ?? []), ...(faneinnhold?.lenker ?? [])];

  return (
    <RedaksjoneltInnholdContainer>
      {veilederinfo.beskrivelse && (
        <>
          <Heading size="medium" level="2">
            Generell informasjon
          </Heading>
          <BodyLong size="large" style={{ whiteSpace: "pre-wrap" }}>
            {veilederinfo.beskrivelse}
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
      <Heading size="medium" level="3">
        Faneinnhold
      </Heading>
      <RedaksjoneltInnholdTabs
        forHvem={
          <DetaljerFane
            tiltakstype={veilederinfo.faneinnhold?.forHvem}
            tiltakstypeAlert={veilederinfo.faneinnhold?.forHvemInfoboks}
            gjennomforing={faneinnhold?.forHvem}
            gjennomforingAlert={faneinnhold?.forHvemInfoboks}
          />
        }
        detaljerOgInnhold={
          <DetaljerFane
            tiltakstype={veilederinfo.faneinnhold?.detaljerOgInnhold}
            tiltakstypeAlert={veilederinfo.faneinnhold?.detaljerOgInnholdInfoboks}
            gjennomforing={faneinnhold?.detaljerOgInnhold}
            gjennomforingAlert={faneinnhold?.detaljerOgInnholdInfoboks}
          />
        }
        pameldingOgVarighet={
          <DetaljerFane
            tiltakstype={veilederinfo.faneinnhold?.pameldingOgVarighet}
            tiltakstypeAlert={veilederinfo.faneinnhold?.pameldingOgVarighetInfoboks}
            gjennomforing={faneinnhold?.pameldingOgVarighet}
            gjennomforingAlert={faneinnhold?.pameldingOgVarighetInfoboks}
          />
        }
        kontaktinfo={
          <DetaljerFane
            tiltakstype={veilederinfo.faneinnhold?.kontaktinfo}
            tiltakstypeAlert={veilederinfo.faneinnhold?.kontaktinfoInfoboks}
            gjennomforing={faneinnhold?.kontaktinfo}
            gjennomforingAlert={faneinnhold?.kontaktinfoInfoboks}
          />
        }
        lenker={lenker.length ? <RedaksjoneltInnholdLenker lenker={lenker} /> : null}
        delMedBruker={
          (faneinnhold?.delMedBruker ?? veilederinfo.faneinnhold?.delMedBruker) ? (
            <BodyLong as="div" size="small" className="prose">
              {faneinnhold?.delMedBruker ?? veilederinfo.faneinnhold?.delMedBruker}
            </BodyLong>
          ) : null
        }
      />
    </RedaksjoneltInnholdContainer>
  );
}

interface DetaljerFaneProps {
  gjennomforingAlert?: string | null;
  tiltakstypeAlert?: string | null;
  gjennomforing?: any;
  tiltakstype?: any;
}

function DetaljerFane({
  gjennomforingAlert,
  tiltakstypeAlert,
  gjennomforing,
  tiltakstype,
}: DetaljerFaneProps) {
  if (!gjennomforingAlert && !tiltakstypeAlert && !gjennomforing && !tiltakstype) {
    return null;
  }

  return (
    <VStack className="mt-4">
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
    </VStack>
  );
}

export function RedaksjoneltInnholdLenker({ lenker }: { lenker: FaneinnholdLenke[] }) {
  return (
    <Box marginBlock="space-16" asChild>
      <List data-aksel-migrated-v8 as="ul">
        {lenker.map((lenke, index) => (
          <List.Item key={index} className="break-words">
            <HStack gap="space-16">
              <LenkeComponent
                to={lenke.lenke}
                target={lenke.apneINyFane ? "_blank" : "_self"}
                isExternal={lenke.apneINyFane}
              >
                {lenke.lenkenavn} ({lenke.lenke})
              </LenkeComponent>
              {lenke.visKunForVeileder ? <small>(Vises kun i Modia)</small> : null}
            </HStack>
          </List.Item>
        ))}
      </List>
    </Box>
  );
}
