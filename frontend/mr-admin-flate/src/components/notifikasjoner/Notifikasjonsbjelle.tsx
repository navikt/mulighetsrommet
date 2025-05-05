import { BellIcon } from "@navikt/aksel-icons";
import { Link } from "react-router";
import { useNotificationSummary } from "@/api/notifikasjoner/useNotificationSummary";

function Notifier() {
  return (
    <span className="w-2 h-2 bg-[var(--a-icon-danger)] inline-block rounded-full absolute right-0.5 bottom-[15px]"></span>
  );
}

export function Notifikasjonsbjelle() {
  const { data: summary, isLoading: isLoadingUlesteNotifikasjoner } = useNotificationSummary();

  if (isLoadingUlesteNotifikasjoner || !summary) {
    return null;
  }

  const harUlesteNotifikasjoner = summary.notDoneCount > 0;

  return (
    <Link to="/oppgaveoversikt/notifikasjoner" className="text-white mr-4">
      <div className="relative flex items-center justify-center">
        {harUlesteNotifikasjoner ? <Notifier /> : null}
        <BellIcon fontSize={24} title="Notifikasjonsbjelle" />
      </div>
    </Link>
  );
}
