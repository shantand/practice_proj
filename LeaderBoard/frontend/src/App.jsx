import { useEffect, useRef, useState } from 'react'
import './App.css'

const POLL_MS = 2000

function formatTime(date) {
  if (!date) {
    return '—'
  }
  return date.toLocaleTimeString()
}

function App() {
  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('connecting')
  const [updatedAt, setUpdatedAt] = useState(null)
  const [leadChange, setLeadChange] = useState(null)
  const previousLeaderRef = useRef(null)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const response = await fetch('/api/leaderboard')
        if (!response.ok) {
          throw new Error('API error')
        }
        const data = await response.json()
        if (cancelled) {
          return
        }

        const leaderId = data[0]?.playerId ?? null
        const previousLeaderId = previousLeaderRef.current
        if (previousLeaderId && leaderId && previousLeaderId !== leaderId) {
          setLeadChange({
            playerId: leaderId,
            previousId: previousLeaderId,
            at: Date.now(),
          })
        }
        previousLeaderRef.current = leaderId

        setRows(data)
        setStatus('live')
        setUpdatedAt(new Date())
      } catch {
        if (!cancelled) {
          setStatus('offline')
        }
      }
    }

    load()
    const timer = setInterval(load, POLL_MS)
    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [])

  useEffect(() => {
    if (!leadChange) {
      return undefined
    }
    const timer = setTimeout(() => setLeadChange(null), 2400)
    return () => clearTimeout(timer)
  }, [leadChange])

  return (
    <main className="page">
      <section className="panel">
        <header className="header">
          <div>
            <h1>Leaderboard</h1>
            <p className="subtitle">Top 100 of 1,000,000 players</p>
          </div>
          <span className={`badge ${status}`}>{status}</span>
        </header>

        <p className="meta">Last update: {formatTime(updatedAt)}</p>

        {leadChange && (
          <p className="lead-banner" key={leadChange.at}>
            <span className="tick" aria-hidden="true">
              ✓
            </span>
            {leadChange.playerId} took 1st from {leadChange.previousId}
          </p>
        )}

        {status === 'offline' && (
          <p className="error">
            Cannot reach the Java API. Start Redis and run{' '}
            <code>mvn compile exec:java</code>.
          </p>
        )}

        <div className="board">
          <div className="row head">
            <p>Rank</p>
            <p>Player</p>
            <p className="score">Score</p>
          </div>
          {rows.length === 0 ? (
            <p className="empty">Waiting for scores…</p>
          ) : (
            rows.map((row) => {
              const tookLead =
                leadChange && row.rank === 1 && row.playerId === leadChange.playerId
              return (
                <div
                  key={row.playerId}
                  className={[
                    'row',
                    `top-${row.rank <= 3 ? row.rank : 'rest'}`,
                    tookLead ? 'lead-change' : '',
                  ].join(' ')}
                >
                  <p className="rank">
                    {row.rank}
                    {tookLead && (
                      <span className="tick" aria-label="new leader">
                        ✓
                      </span>
                    )}
                  </p>
                  <p className="player">{row.playerId}</p>
                  <p className="score">{row.score}</p>
                </div>
              )
            })
          )}
        </div>
      </section>
    </main>
  )
}

export default App
