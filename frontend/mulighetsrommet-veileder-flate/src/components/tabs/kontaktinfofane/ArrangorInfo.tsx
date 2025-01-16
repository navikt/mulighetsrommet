import { PortableTextTypedObject, VeilderflateArrangor } from "@mr/api-client";
import { BodyLong, BodyShort, Heading, Link } from "@navikt/ds-react";
import { RedaksjoneltInnhold } from "../../RedaksjoneltInnhold";

interface ArrangorInfoProps {
  arrangor: VeilderflateArrangor;
  faneinnhold?: Array<PortableTextTypedObject>;
}

const ArrangorInfo = ({ arrangor, faneinnhold }: ArrangorInfoProps) => {
  const { kontaktpersoner } = arrangor;

  return (
    <div>
      <Heading size="small" spacing={false}>
        Arrangør
      </Heading>

      <BodyShort spacing size="small">
        {arrangor.selskapsnavn}
      </BodyShort>

      {kontaktpersoner.map((person) => (
        <div key={person.id} className="prose bg-bg-subtle p-2 my-2 rounded-md">
          <BodyShort className="font-bold">{person.navn}</BodyShort>
          {person.beskrivelse && (
            <BodyShort textColor="subtle" size="small">
              {person.beskrivelse}
            </BodyShort>
          )}

          <BodyShort as="div" size="small">
            <dl>
              <dt>Epost:</dt>
              <dd>
                <Link href={`mailto:${person.epost}`}>{person.epost}</Link>
              </dd>
              {person.telefon ? (
                <>
                  <dt>Telefon:</dt>
                  <dd>
                    <span>{person.telefon}</span>
                  </dd>
                </>
              ) : null}
            </dl>
          </BodyShort>
        </div>
      ))}
      {faneinnhold && (
        <BodyLong as="div" textColor="subtle" size="small">
          <RedaksjoneltInnhold value={faneinnhold} />
        </BodyLong>
      )}
    </div>
  );
};
export default ArrangorInfo;
