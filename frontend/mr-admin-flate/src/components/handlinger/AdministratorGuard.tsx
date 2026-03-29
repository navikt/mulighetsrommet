import { ConfirmModal } from "./ConfirmModal";

interface Props {
  administratorer: Array<string>;
  navIdent: string;
  children: React.ReactElement<{ onClick?: (e: React.MouseEvent) => void }>;
}

export function AdministratorGuard({ administratorer, navIdent, children }: Props) {
  if (administratorer.length === 0 || administratorer.includes(navIdent)) {
    return <>{children}</>;
  } else {
    return (
      <ConfirmModal heading="Du er ikke satt som administrator" body="Vil du fortsette?">
        {children}
      </ConfirmModal>
    );
  }
}
