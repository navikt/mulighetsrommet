import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import SlettModal from '../../components/modal/SlettModal';
import useTiltaksvariant from '../../hooks/tiltaksvariant/useTiltaksvariant';
import useTiltaksvariantDelete from '../../hooks/tiltaksvariant/useTiltaksvariantDelete';
import useTiltaksvariantUpdate from '../../hooks/tiltaksvariant/useTiltaksvariantUpdate';
import MainView from '../../layouts/MainView';
import TiltaksvariantForm from './TiltaksvariantForm';

export const RedigerTiltaksvariant = () => {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  const [isModalOpen, setIsModalOpen] = useState(false);

  const { data, isLoading, isError, isSuccess } = useTiltaksvariant(id);

  const edit = useTiltaksvariantUpdate();
  const deleteMutation = useTiltaksvariantDelete();

  return (
    <MainView
      title="Rediger tiltaksvariant"
      dataTestId="header-rediger-tiltaksvariant"
      tilbakelenke={'/tiltaksvarianter/' + id}
    >
      <TiltaksvariantForm
        isLoading={isLoading}
        isError={isError}
        isSuccess={isSuccess}
        tiltaksvariant={data}
        onSubmit={requestBody => edit.mutate({ id, requestBody })}
        onDelete={() => setIsModalOpen(true)}
        isEdit={true}
      />
      <SlettModal
        tittel="Slett tiltaksvariant"
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        handleDelete={() => deleteMutation.mutate({ id })}
      />
    </MainView>
  );
};
