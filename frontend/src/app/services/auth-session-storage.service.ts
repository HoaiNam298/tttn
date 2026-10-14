import { Injectable } from '@angular/core';
import { StoredAuthSession } from '../models/stored-auth-session.model';
import { SessionActivity } from '../models/session-activity.model';

@Injectable({ providedIn: 'root' })
export class AuthSessionStorage {
  private readonly storageKey = 'shopapp_keycloak_session_v1';
  private readonly activityKey = 'shopapp_keycloak_activity_v1';
  available = true;

  readTokens(): StoredAuthSession | null {
    let raw: string | null;
    try {
      raw = localStorage.getItem(this.storageKey);
    } catch {
      this.available = false;
      return null;
    }
    try {
      const saved = JSON.parse(raw ?? 'null') as StoredAuthSession | null;
      return saved &&
        typeof saved.id === 'string' &&
        typeof saved.accessToken === 'string' &&
        typeof saved.refreshToken === 'string' &&
        !!saved.id &&
        !!saved.accessToken &&
        !!saved.refreshToken &&
        Number.isFinite(saved.expiresAt)
        ? saved
        : null;
    } catch {
      return null;
    }
  }

  readActivity(): SessionActivity | null {
    try {
      const activity = JSON.parse(
        localStorage.getItem(this.activityKey) ?? 'null',
      ) as SessionActivity | null;
      return activity &&
        typeof activity.sessionId === 'string' &&
        Number.isFinite(activity.at) &&
        activity.at <= Date.now()
        ? activity
        : null;
    } catch {
      return null;
    }
  }

  writeTokens(stored: StoredAuthSession): void {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(stored));
    } catch {
      this.available = false;
    }
  }

  writeActivity(id: string, at: number): void {
    try {
      localStorage.setItem(
        this.activityKey,
        JSON.stringify({ sessionId: id, at } satisfies SessionActivity),
      );
    } catch {
      // The in-memory deadline still applies without browser storage.
    }
  }

  removeSession(id: string | undefined): void {
    try {
      const saved = this.readTokens();
      // An old tab must never remove a newer login.
      if (!saved || saved.id === id) {
        localStorage.removeItem(this.storageKey);
        localStorage.removeItem(this.activityKey);
      }
    } catch {
      this.available = false;
    }
  }

  isSessionEvent(event: StorageEvent): boolean {
    return event.key === this.storageKey || event.key === null;
  }

  clearLegacyStorage(): void {
    try {
      sessionStorage.removeItem('shopapp_keycloak_refresh_v1');
      localStorage.removeItem('shopapp_auth');
    } catch {
      // Ignore obsolete storage when persistence is unavailable.
    }
  }
}
