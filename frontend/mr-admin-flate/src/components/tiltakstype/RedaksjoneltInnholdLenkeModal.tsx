import {
  Button,
  Heading,
  HStack,
  List,
  LocalAlert,
  Modal,
  Table,
  TextField,
  VStack,
} from "@navikt/ds-react";
import { useState } from "react";
import { useRedaksjoneltInnholdLenker } from "@/api/redaksjonelt-innhold/useRedaksjoneltInnholdLenker";
import { useUpsertRedaksjoneltInnholdLenke } from "@/api/redaksjonelt-innhold/useUpsertRedaksjoneltInnholdLenke";
import { useDeleteRedaksjoneltInnholdLenke } from "@/api/redaksjonelt-innhold/useDeleteRedaksjoneltInnholdLenke";
import { RedaksjoneltInnholdLenke, ValidationError } from "@tiltaksadministrasjon/api-client";

interface Props {
  open: boolean;
  onClose: () => void;
}

interface RowValues {
  url: string;
  navn: string;
  beskrivelse: string;
}

const emptyRow: RowValues = { url: "", navn: "", beskrivelse: "" };

export function RedaksjoneltInnholdLenkeModal({ open, onClose }: Props) {
  const lenker = useRedaksjoneltInnholdLenker();
  const deleteMutation = useDeleteRedaksjoneltInnholdLenke();

  const [editing, setEditing] = useState<string | null>(null);
  const [values, setValues] = useState<RowValues>(emptyRow);
  const [deleteError, setDeleteError] = useState<ValidationError | null>(null);

  const isNewRow = editing !== null && !lenker.some((l) => l.id === editing);

  function addNewRow() {
    setEditing(crypto.randomUUID());
    setValues(emptyRow);
  }

  function editRow(lenke: RedaksjoneltInnholdLenke) {
    setEditing(lenke.id);
    setValues({ url: lenke.url, navn: lenke.navn ?? "", beskrivelse: lenke.beskrivelse ?? "" });
  }

  function cancel() {
    setEditing(null);
    setValues(emptyRow);
  }

  return (
    <Modal width={900} aria-label="Administrer lenker" open={open} onClose={onClose} portal>
      <Modal.Header>
        <Heading size="medium">Administrer lenker</Heading>
      </Modal.Header>
      <Modal.Body>
        <VStack gap="space-8">
          {deleteError && (
            <LocalAlert status="error">
              <LocalAlert.Header>
                <LocalAlert.Title as="h3">{deleteError.detail}</LocalAlert.Title>
                <LocalAlert.CloseButton onClick={() => setDeleteError(null)} />
              </LocalAlert.Header>
              <LocalAlert.Content>
                <List>
                  {deleteError.errors.map((e, i) => (
                    <List.Item key={i}>{e.detail}</List.Item>
                  ))}
                </List>
              </LocalAlert.Content>
            </LocalAlert>
          )}
          <Table size="small">
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>URL</Table.HeaderCell>
                <Table.HeaderCell>Lenketekst</Table.HeaderCell>
                <Table.HeaderCell>Beskrivelse</Table.HeaderCell>
                <Table.HeaderCell />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {lenker.map((lenke) =>
                editing === lenke.id ? (
                  <EditRedaksjoneltInnholdLenke
                    key={lenke.id}
                    id={editing}
                    values={values}
                    onChange={setValues}
                    onSave={cancel}
                    onCancel={cancel}
                  />
                ) : (
                  <Table.Row key={lenke.id}>
                    <Table.DataCell>{lenke.url}</Table.DataCell>
                    <Table.DataCell>{lenke.navn}</Table.DataCell>
                    <Table.DataCell>{lenke.beskrivelse}</Table.DataCell>
                    <Table.DataCell>
                      <HStack gap="space-4" justify="end">
                        <Button
                          size="xsmall"
                          variant="tertiary"
                          onClick={() => editRow(lenke)}
                          disabled={editing !== null}
                        >
                          Rediger
                        </Button>
                        <Button
                          size="xsmall"
                          variant="tertiary-neutral"
                          onClick={() =>
                            deleteMutation.mutate(lenke.id, {
                              onValidationError: (error) => setDeleteError(error),
                            })
                          }
                          loading={deleteMutation.isPending}
                          disabled={editing !== null}
                        >
                          Slett
                        </Button>
                      </HStack>
                    </Table.DataCell>
                  </Table.Row>
                ),
              )}
              {isNewRow && (
                <EditRedaksjoneltInnholdLenke
                  id={editing}
                  values={values}
                  onChange={setValues}
                  onSave={cancel}
                  onCancel={cancel}
                />
              )}
            </Table.Body>
          </Table>
          {editing === null && (
            <Button size="small" variant="secondary" onClick={addNewRow}>
              Legg til ny lenke
            </Button>
          )}
        </VStack>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="primary" onClick={onClose}>
          Lukk
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

interface EditRedaksjoneltInnholdLenkeProps {
  id: string;
  values: RowValues;
  onChange: (v: RowValues) => void;
  onSave: () => void;
  onCancel: () => void;
}

function EditRedaksjoneltInnholdLenke({
  id,
  values,
  onChange,
  onSave,
  onCancel,
}: EditRedaksjoneltInnholdLenkeProps) {
  const upsertMutation = useUpsertRedaksjoneltInnholdLenke(id);

  function save() {
    upsertMutation.mutate(
      { url: values.url, navn: values.navn || null, beskrivelse: values.beskrivelse || null },
      { onSuccess: onSave },
    );
  }

  return (
    <Table.Row>
      <Table.DataCell>
        <TextField
          size="small"
          label="URL"
          hideLabel
          value={values.url}
          onChange={(e) => onChange({ ...values, url: e.target.value })}
        />
      </Table.DataCell>
      <Table.DataCell>
        <TextField
          size="small"
          label="Lenketekst"
          hideLabel
          value={values.navn}
          onChange={(e) => onChange({ ...values, navn: e.target.value })}
        />
      </Table.DataCell>
      <Table.DataCell>
        <TextField
          size="small"
          label="Beskrivelse"
          hideLabel
          value={values.beskrivelse}
          onChange={(e) => onChange({ ...values, beskrivelse: e.target.value })}
        />
      </Table.DataCell>
      <Table.DataCell>
        <HStack gap="space-4" justify="end">
          <Button size="xsmall" variant="primary" onClick={save} loading={upsertMutation.isPending}>
            Lagre
          </Button>
          <Button size="xsmall" variant="tertiary-neutral" onClick={onCancel}>
            Avbryt
          </Button>
        </HStack>
      </Table.DataCell>
    </Table.Row>
  );
}
