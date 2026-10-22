import { environment } from '../environments/environment';

export function isShopAppApi(
  url: string | null | undefined,
  baseUri: string,
): boolean {
  if (!url) {
    return false;
  }
  try {
    const api = new URL(environment.apiUrl, baseUri);
    const target = new URL(url, baseUri);
    return (
      target.origin === api.origin &&
      (target.pathname === api.pathname ||
        target.pathname.startsWith(`${api.pathname}/`))
    );
  } catch {
    return false;
  }
}
