import { useState } from 'react'
import MoodSelector from './components/MoodSelector'
import MusicRecommendations from './components/MusicRecommendations'
import './styles/MoodMusic.css'

function App() {
  const [selectedMood, setSelectedMood] = useState(null)
  const [showRecommendations, setShowRecommendations] = useState(false)

  const handleMoodSelect = (mood) => {
    setSelectedMood(mood)
    setShowRecommendations(true)
  }

  const handleBackToMoods = () => {
    setShowRecommendations(false)
    setSelectedMood(null)
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1 className="app-title">
            <span className="music-note">♪</span>
            MoodTunes
            <span className="music-note">♫</span>
          </h1>
          <p className="app-subtitle">Discover music that matches your vibe</p>
        </div>
      </header>

      <main className="app-main">
        {!showRecommendations ? (
          <div className="center-content">
            <MoodSelector onMoodSelect={handleMoodSelect} />
          </div>
        ) : (
          <MusicRecommendations 
            mood={selectedMood} 
            onBack={handleBackToMoods}
          />
        )}
      </main>

      <footer className="app-footer">
        <p>&copy; 2025 MoodTunes - Find your perfect soundtrack</p>
      </footer>
    </div>
  )
}

export default App