export const environment = {
  apiUrl: '/api/v1',
  keycloak: {
    url: 'http://localhost:8081',
    realm: 'shopapp',
    clientId: 'shopapp-web',
  },
} as const;
