import { PortableTextTypedObject, VeilederflateArrangor } from "@arbeidsmarkedstiltak/api-client";
import { PortableText } from "@mr/frontend-common";
import { BodyLong, BodyShort, Heading, Link } from "@navikt/ds-react";
import { KontaktpersonBox } from "./KontaktpersonBox";

interface ArrangorInfoProps {
  arrangor: VeilederflateArrangor;
  faneinnhold?: Array<PortableTextTypedObject> | null;
}

export function ArrangorInfo({ arrangor, faneinnhold }: ArrangorInfoProps) {
  const { kontaktpersoner } = arrangor;

  return (
    <div>
      <Heading size="small" spacing={false}>
        Arrangør
      </Heading>

      <BodyShort spacing={false} size="small">
        {arrangor.selskapsnavn}
      </BodyShort>

      {kontaktpersoner.map((person) => (
        <KontaktpersonBox key={person.id}>
          <Heading level="4" size="xsmall" className="font-bold">
            {person.navn}
          </Heading>
          {person.beskrivelse && (
            <BodyShort textColor="subtle" size="small">
              {person.beskrivelse}
            </BodyShort>
          )}
          <BodyShort as="div" size="small">
            <dl className="flex flex-col gap-1">
              <div>
                <dt className="inline">Epost: </dt>
                <dd className="inline">
                  <Link href={`mailto:${person.epost}`}>{person.epost}</Link>
                </dd>
              </div>
              {person.telefon ? (
                <div>
                  <dt className="inline">Telefon: </dt>
                  <dd className="inline">{person.telefon}</dd>
                </div>
              ) : null}
            </dl>
          </BodyShort>
        </KontaktpersonBox>
      ))}
      {faneinnhold && (
        <BodyLong as="div" textColor="subtle" size="small">
          <PortableText value={faneinnhold} />
        </BodyLong>
      )}
    </div>
  );
}
