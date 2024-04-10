import { useNavigate, useParams } from "react-router-dom";
import { ArrangorPage } from "./ArrangorPage";

export function ArrangorPageContainer() {
  const { arrangorId } = useParams();
  const navigate = useNavigate();

  if (!arrangorId) {
    navigate("/arrangorer");
    return null;
  }

  return <ArrangorPage arrangorId={arrangorId} />;
}
