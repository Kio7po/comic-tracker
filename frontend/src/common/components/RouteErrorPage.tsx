import { isRouteErrorResponse, useRouteError } from 'react-router';
import ErrorPage from './ErrorPage';

function RouteErrorPage() {
  const error = useRouteError();
  const status = isRouteErrorResponse(error) ? error.status : 500;

  return <ErrorPage status={status} />;
}

export default RouteErrorPage;