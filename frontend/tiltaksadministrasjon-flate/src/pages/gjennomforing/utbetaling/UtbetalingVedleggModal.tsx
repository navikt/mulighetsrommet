import { useVedleggPdfBlob } from "@/api/utbetaling/useVedleggPdfBlob";
import { ForhandsvisningPdfModal } from "@/components/tilskudd-behandling/ForhandsvisningPdfModal";

interface Props {
  open: boolean;
  onClose: () => void;
  dokumentInfoId: string;
  utbetalingId: string;
  title: string;
}

export function UtbetalingVedleggModal({
  open,
  onClose,
  title,
  dokumentInfoId,
  utbetalingId,
}: Props) {
  const { data: blob, isLoading, isError } = useVedleggPdfBlob(utbetalingId, dokumentInfoId);
  return (
    <ForhandsvisningPdfModal
      open={open}
      onClose={onClose}
      title={title}
      blob={blob}
      isLoading={isLoading}
      isError={isError}
    />
  );
}
