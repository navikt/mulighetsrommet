import { zodResolver } from "@hookform/resolvers/zod";
import { BodyShort, Button, Heading, Modal, TextField } from "@navikt/ds-react";
import { useEffect, useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import z from "zod";

const LagreFilterSchema = z.object({
  navn: z
    .string({ required_error: "Du må gi filteret et navn" })
    .min(3, "Du må gi filteret et navn")
    .max(100, "Navnet kan ikke være lengre enn 100 tegn"),
});

type InferredLagreFilterSchema = z.infer<typeof LagreFilterSchema>;

interface Props {
  filter: { [key: string]: unknown };
  onLagre: (filter: NamedFilter) => void;
}

interface NamedFilter {
  id: string;
  navn: string;
  filter: { [key: string]: unknown };
}

export function LagreFilterButton({ onLagre, filter }: Props) {
  const [isOpen, setIsOpen] = useState(false);

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
    onLagre({
      id: window.crypto.randomUUID(),
      navn: data.navn,
      filter,
    });
    setIsOpen(false);
    form.reset();
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
        aria-label="Lagre nytt filter"
        title="Lagre nytt filter"
      >
        <FormProvider {...form}>
          <form onSubmit={handleSubmit(lagreFilter)}>
            <Modal.Header closeButton>
              <Heading size="medium">Lagre nytt filter</Heading>
            </Modal.Header>
            <Modal.Body>
              <BodyShort>Du vil finne igjen filteret under "Lagrede filter".</BodyShort>
              <TextField {...register("navn")} error={errors.navn?.message} label="Navn:" />
            </Modal.Body>
            <Modal.Footer>
              <Button type="submit" variant="primary">
                Lagre
              </Button>
            </Modal.Footer>
          </form>
        </FormProvider>
      </Modal>
    </>
  );
}
