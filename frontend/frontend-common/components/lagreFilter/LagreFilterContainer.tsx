import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Button, Heading, Modal, TextField } from "@navikt/ds-react";
import { LagretDokumenttype } from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import z from "zod";
import { useLagreFilter } from "./useLagreFilter";

const LagreFilterSchema = z.object({
  navn: z
    .string({ required_error: "Du må gi filteret et navn" })
    .min(3, "Du må gi filteret et navn")
    .max(100, "Navnet kan ikke være lengre enn 100 tegn"),
});

type InferredLagreFilterSchema = z.infer<typeof LagreFilterSchema>;

interface Props {
  dokumenttype: LagretDokumenttype;
  filter: any;
}

export function LagreFilterContainer({ dokumenttype, filter }: Props) {
  const [isOpen, setIsOpen] = useState(false);
  const mutation = useLagreFilter({
    onSuccess: () => setIsOpen(false),
    dokumenttype,
  });

  const form = useForm<InferredLagreFilterSchema>({
    resolver: zodResolver(LagreFilterSchema),
  });

  const {
    handleSubmit,
    register,
    formState: { errors },
    setFocus,
  } = form;

  useEffect(() => {
    setFocus("navn");
  }, [setFocus, isOpen]);

  function lagreFilter(data: InferredLagreFilterSchema) {
    mutation.mutate(
      {
        navn: data.navn,
        filter,
        type: dokumenttype,
        sortOrder: 0,
        id: window.crypto.randomUUID(),
      },
      {
        onSuccess: () => {
          mutation.reset();
          form.reset();
        },
      },
    );
  }

  return (
    <>
      <Button variant="tertiary" size="small" onClick={() => setIsOpen(true)}>
        Lagre filter
      </Button>
      <Modal
        width={500}
        closeOnBackdropClick
        onClose={() => setIsOpen(false)}
        open={isOpen}
        aria-label="Lagre filter"
        title="Lagre filter"
      >
        <FormProvider {...form}>
          <form onSubmit={handleSubmit(lagreFilter)}>
            <Modal.Header closeButton>
              <Heading size="medium">Lagre filter</Heading>
            </Modal.Header>
            <Modal.Body>
              <TextField
                {...register("navn")}
                size="small"
                error={errors.navn?.message}
                label="Navn på filter"
              />
              {mutation.error ? (
                <Alert style={{ marginTop: "2rem" }} variant="error">
                  Klarte ikke lagre filter
                </Alert>
              ) : null}
            </Modal.Body>
            <Modal.Footer>
              <Button type="submit" size="small" variant="primary">
                Lagre
              </Button>
            </Modal.Footer>
          </form>
        </FormProvider>
      </Modal>
    </>
  );
}
