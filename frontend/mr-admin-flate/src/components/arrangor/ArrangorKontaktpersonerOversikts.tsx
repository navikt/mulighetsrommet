import { Button, HStack, Loader, Table, TextField } from "@navikt/ds-react";
import { Arrangor, ArrangorKontaktperson } from "mulighetsrommet-api-client";
import { useState } from "react";
import { z } from "zod";
import { useArrangorKontaktpersoner } from "../../api/arrangor/useArrangorKontaktpersoner";
import { useUpsertArrangorKontaktperson } from "../../api/arrangor/useUpsertArrangorKontaktperson";
import { useHandleApiUpsertResponse } from "../../api/effects";
import { SlettKontaktpersonModal } from "./SlettKontaktpersonModal";

interface Props {
  arrangor: Arrangor;
}

export function ArrangorKontaktpersonOversikt({ arrangor }: Props) {
  const { data, isLoading } = useArrangorKontaktpersoner(arrangor.id);
  const [redigerKontaktpersonId, setRedigerKontaktpersonId] = useState<string | undefined>(
    undefined,
  );
  const [slettKontaktperson, setSlettKontaktperson] = useState<ArrangorKontaktperson | undefined>(
    undefined,
  );

  if (!data || isLoading) {
    return <Loader />;
  }

  return (
    <div>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Kontaktperson</Table.HeaderCell>
            <Table.HeaderCell>Telefon</Table.HeaderCell>
            <Table.HeaderCell>E-post</Table.HeaderCell>
            <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
            <Table.HeaderCell>{""}</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {data.map((kontaktperson) =>
            redigerKontaktpersonId === kontaktperson.id ? (
              <RedigerbarRad
                key={kontaktperson.id}
                kontaktperson={kontaktperson}
                setRedigerKontaktperson={setRedigerKontaktpersonId}
                arrangor={arrangor}
              />
            ) : (
              <LeseRad
                key={kontaktperson.id}
                kontaktperson={kontaktperson}
                setRedigerKontaktperson={setRedigerKontaktpersonId}
                setSlettKontaktperson={() => setSlettKontaktperson(kontaktperson)}
              />
            ),
          )}
        </Table.Body>
      </Table>
      {slettKontaktperson ? (
        <SlettKontaktpersonModal
          onClose={() => setSlettKontaktperson(undefined)}
          kontaktperson={slettKontaktperson}
        />
      ) : null}
    </div>
  );
}

interface ILeseRad {
  kontaktperson: ArrangorKontaktperson;
  setRedigerKontaktperson: (kontaktpersonId: string | undefined) => void;
  setSlettKontaktperson: (kontaktperson: ArrangorKontaktperson | undefined) => void;
}

function LeseRad({ kontaktperson, setRedigerKontaktperson, setSlettKontaktperson }: ILeseRad) {
  return (
    <Table.Row key={kontaktperson.id}>
      <Table.DataCell>{kontaktperson.navn}</Table.DataCell>
      <Table.DataCell>
        <a href={`tel:${kontaktperson.telefon}`}>{kontaktperson.telefon}</a>
      </Table.DataCell>
      <Table.DataCell>
        <a href={`mailto:${kontaktperson.epost}`}>{kontaktperson.epost}</a>
      </Table.DataCell>
      <Table.DataCell>{kontaktperson.beskrivelse}</Table.DataCell>
      <Table.DataCell>
        <HStack gap="5">
          <Button
            onClick={() => setRedigerKontaktperson(kontaktperson.id)}
            variant="secondary"
            size="small"
          >
            Rediger
          </Button>
          <Button
            variant="danger"
            size="small"
            onClick={() => setSlettKontaktperson(kontaktperson)}
          >
            Slett
          </Button>
        </HStack>
      </Table.DataCell>
    </Table.Row>
  );
}

interface RedigerbarRadProps extends Pick<ILeseRad, "setRedigerKontaktperson" | "kontaktperson"> {
  arrangor: Arrangor;
}

const KontaktpersonSchema = z.object({
  id: z.string(),
  arrangorId: z.string(),
  navn: z
    .string()
    .min(3, "Navn må være minst tre tegn langt")
    .max(100, "Navn kan ikke være mer enn 100 tegn langt"),
  epost: z.string().email("Du må skrive inn en gyldig e-postadresse").optional(),
  telefon: z.string().optional(),
  beskrivelse: z.string().optional(),
});

interface State extends ArrangorKontaktperson {
  errors: Record<string, string>;
}

function RedigerbarRad({ kontaktperson, setRedigerKontaktperson, arrangor }: RedigerbarRadProps) {
  const defaultState: State = {
    id: kontaktperson.id,
    arrangorId: arrangor.id,
    navn: kontaktperson.navn || "",
    epost: kontaktperson.epost || "",
    telefon: kontaktperson.telefon || "",
    beskrivelse: kontaktperson.beskrivelse || "",
    errors: {},
  };

  const [state, setState] = useState<State>(defaultState);
  const mutation = useUpsertArrangorKontaktperson(arrangor.id);

  function updateField(field: keyof ArrangorKontaktperson, value: string) {
    setState((prevKontaktperson) => ({ ...prevKontaktperson, [field]: value }));
  }

  function lagre() {
    const result = KontaktpersonSchema.safeParse(state);
    if (!result.success) {
      const errors = result.error.errors.reduce((prev, next) => {
        return { ...prev, [next.path[0]]: next.message };
      }, {});

      setState((prevKontaktperson) => ({
        ...prevKontaktperson,
        errors,
      }));

      return;
    }
    mutation.mutate(state);
  }

  useHandleApiUpsertResponse(
    mutation,
    () => {
      setRedigerKontaktperson(undefined);
      mutation.reset();
    },
    (validation) => {
      const errors = validation.errors.reduce((errors: Record<string, string>, error) => {
        return { ...errors, [error.name]: error.message };
      }, {});
      setState({ ...state, errors });
    },
  );

  return (
    <Table.Row key={kontaktperson.id}>
      <Table.DataCell>
        <TextField
          label="Navn på kontaktperson"
          placeholder="Fullt navn på kontaktperson"
          hideLabel
          size="small"
          value={state.navn}
          onChange={(e) => updateField("navn", e.target.value)}
          error={state.errors.navn}
          autoFocus
        />
      </Table.DataCell>
      <Table.DataCell>
        <TextField
          label="Telefonnummer"
          placeholder="Telefonnummer"
          hideLabel
          size="small"
          value={state.telefon || undefined}
          onChange={(e) => updateField("telefon", e.target.value)}
        />
      </Table.DataCell>
      <Table.DataCell>
        <TextField
          label="E-post"
          placeholder="E-post"
          hideLabel
          size="small"
          value={state.epost || undefined}
          onChange={(e) => updateField("epost", e.target.value)}
          error={state.errors.epost}
        />
      </Table.DataCell>
      <Table.DataCell>
        {" "}
        <TextField
          label="Beskrivelse"
          placeholder="Beskrivelse"
          hideLabel
          size="small"
          value={state.beskrivelse || undefined}
          onChange={(e) => updateField("beskrivelse", e.target.value)}
        />
      </Table.DataCell>
      <Table.DataCell>
        <HStack gap="5">
          <Button onClick={lagre} variant="primary" size="small">
            Lagre
          </Button>
          <Button onClick={() => setRedigerKontaktperson(undefined)} variant="danger" size="small">
            Avbryt
          </Button>
        </HStack>
      </Table.DataCell>
    </Table.Row>
  );
}
