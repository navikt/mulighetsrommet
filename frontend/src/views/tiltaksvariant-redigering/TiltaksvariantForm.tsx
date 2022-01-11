import { Delete, Edit } from '@navikt/ds-icons';
import React from 'react';
import { useForm } from 'react-hook-form';
import FormInput from '../../components/form-elements/FormInput';
import { Tiltaksvariant } from '../../core/domain/Tiltaksvariant';
import { useInnsatsgrupper } from '../../hooks/tiltaksvariant/useInnsatsgrupper';
import './TiltaksvariantForm.less';
import { ReactComponent as AddCircle } from '../../ikoner/AddCircle.svg';
import { FormSelect } from '../../components/form-elements/FormSelect';
import { Alert, Button, Loader } from '@navikt/ds-react';

interface TiltaksvariantFormProps {
  isLoading?: boolean;
  isError?: boolean;
  isSuccess?: boolean;
  tiltaksvariant?: Tiltaksvariant;
  onSubmit: (tiltaksvariant: Tiltaksvariant) => void;
  onDelete?: () => void;
  setModalOpen?: (open: boolean) => void;
  isEdit: boolean;
}

const TiltaksvariantForm = ({
  isLoading,
  isError,
  isSuccess,
  tiltaksvariant,
  onSubmit,
  onDelete,
  isEdit,
}: TiltaksvariantFormProps) => {
  const { data: innsatsgrupper = [] } = useInnsatsgrupper();

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
  } = useForm<Tiltaksvariant>({
    defaultValues: { innsatsgruppe: null, ...tiltaksvariant },
  });

  const tomtFeltErrorMessage = 'Dette feltet kan ikke være tomt';

  return (
    <>
      {isLoading && <Loader variant="neutral" size="2xlarge" />}
      {isError && <Alert variant="error">Det har oppstått en feil</Alert>}
      {(!isEdit || (isEdit && isSuccess)) && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="rediger-opprett-tiltaksvariant__form"
          data-testid="form__rediger-opprett"
        >
          <FormSelect
            name="innsatsgruppe"
            control={control}
            nullable
            label="Innsatsgruppe"
            placeholder="Ingen innsatsgruppe"
            options={innsatsgrupper.map(innsatsgruppe => ({ value: innsatsgruppe.id, label: innsatsgruppe.tittel }))}
            dataTestId="input_innsatsgruppe"
          />
          <FormInput
            label="Tittel"
            register={register('tittel', {
              required: tomtFeltErrorMessage,
              maxLength: { value: 50, message: 'Maks 50 tegn.' },
            })}
            defaultValue={tiltaksvariant ? tiltaksvariant.tittel : ''}
            feil={errors.tittel && errors.tittel.message}
            className="rediger-opprett-tiltaksvariant__form__tittel"
            data-testid="input_tittel"
          />
          <FormInput
            label="Ingress"
            register={register('ingress', {
              required: tomtFeltErrorMessage,
              maxLength: { value: 250, message: 'Maks 250 tegn.' },
            })}
            defaultValue={tiltaksvariant ? tiltaksvariant.ingress : ''}
            feil={errors.ingress && errors.ingress.message}
            className="rediger-opprett-tiltaksvariant__form__ingress"
            data-testid="input_ingress"
          />
          <FormInput
            label="Beskrivelse"
            register={register('beskrivelse', { required: tomtFeltErrorMessage })}
            defaultValue={tiltaksvariant ? tiltaksvariant.beskrivelse : ''}
            feil={errors.beskrivelse && errors.beskrivelse.message}
            className="rediger-opprett-tiltaksvariant__form__beskrivelse"
            data-testid="input_beskrivelse"
          />

          <div className="knapperad">
            {isEdit ? (
              <>
                <Button variant="primary" data-testid="submit-knapp_rediger-tiltaksvariant" type="submit">
                  Rediger tiltaksvariant <Edit title="Blyant" />
                </Button>
                <Button
                  variant="danger"
                  onClick={onDelete}
                  type="button"
                  data-testid="slett-knapp_rediger-tiltaksvariant"
                >
                  Slett tiltaksvariant <Delete title="Søppelkasse" />
                </Button>
              </>
            ) : (
              <Button variant="primary" data-testid="submit-knapp_opprett-tiltaksvariant">
                Opprett tiltaksvariant <AddCircle />
              </Button>
            )}
          </div>
        </form>
      )}
    </>
  );
};

export default TiltaksvariantForm;
